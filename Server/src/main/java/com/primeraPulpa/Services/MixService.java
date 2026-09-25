package com.primeraPulpa.Services;

import com.primeraPulpa.dto.DesgloseCostoMixDTO;
import com.primeraPulpa.dto.DesgloseElaboracionDTO;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MixService extends BaseService<Mix, Long> {

    private final DetallePedidoRepository detallePedidoRepository;
    private final FormulaRepository formulaRepository;
    private final DetalleIngresoMPRepository detalleIngresoMPRepository;
    private final HistorialPrecioMixRepository historialPrecioRepository;
    private final CostoAdicionalRepository costoAdicionalRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final DetalleConsumoLoteRepository consumoLoteRepository;

    public MixService(MixRepository repository, DetallePedidoRepository detallePedidoRepository,
                      FormulaRepository formulaRepository,
                      DetalleIngresoMPRepository detalleIngresoMPRepository,
                      HistorialPrecioMixRepository historialPrecioRepository,
                      CostoAdicionalRepository costoAdicionalRepository,
                      MateriaPrimaRepository materiaPrimaRepository,
                      DetalleConsumoLoteRepository consumoLoteRepository) {
        super(repository);
        this.detallePedidoRepository = detallePedidoRepository;
        this.formulaRepository = formulaRepository;
        this.detalleIngresoMPRepository = detalleIngresoMPRepository;
        this.historialPrecioRepository = historialPrecioRepository;
        this.costoAdicionalRepository = costoAdicionalRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.consumoLoteRepository = consumoLoteRepository;
    }

    @Override
    protected void validar(Mix mix) throws ErrorServiceException {
        if (mix.getNombre() == null || mix.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del mix");
        }
        if (mix.getPrecioVenta() != null && mix.getPrecioVenta() < 0) {
            throw new ErrorServiceException("El precio de venta no puede ser negativo");
        }
        if (mix.getCantidadPorUnidad() != null && mix.getCantidadPorUnidad() <= 0) {
            throw new ErrorServiceException("El tamaño de la unidad (kg por paquete) debe ser mayor a cero");
        }
    }

    // Normaliza los campos nuevos a valores por defecto razonables.
    @Override
    protected void preAlta(Mix mix) throws ErrorServiceException {
        normalizarPresentacion(mix);
        mix.setStock(0.0);
        mix.setCosto(0.0);
        mix.setEliminado(false);
    }

    @Override
    protected void preModificacion(Mix mix) throws ErrorServiceException {
        normalizarPresentacion(mix);
    }

    // El formulario de edición no incluye stock, costo ni estado de baja:
    // se conservan los valores persistidos para que la edición no los pise (ej. el stock pasaba a 0).
    @Override
    public Optional<Mix> modificar(Long id, Mix entidadNueva) throws ErrorServiceException {
        try {
            validar(entidadNueva);
            entidadNueva.setId(id);
            preModificacion(entidadNueva);
            return repository.findById(id).map(entidad -> {
                entidadNueva.setStock(entidad.getStock());
                entidadNueva.setCosto(entidad.getCosto());
                entidadNueva.setEliminado(entidad.getEliminado());
                Mix actualizado = repository.save(entidadNueva);
                postModificacion(actualizado);
                return actualizado;
            });
        } catch (ErrorServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ErrorServiceException("Error de Sistemas");
        }
    }

    private void normalizarPresentacion(Mix mix) {
        if (mix.getCantidadPorUnidad() == null || mix.getCantidadPorUnidad() <= 0) {
            mix.setCantidadPorUnidad(1.0);
        }
    }

    // HU-08: no se puede dar de baja un mix con pedidos pendientes asociados
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        Map<Long, Double> pendientes = cantidadesPendientesPorMix();
        double cantidadPendiente = pendientes.getOrDefault(id, 0.0);
        if (cantidadPendiente > 0) {
            throw new ErrorServiceException(
                    "No se puede dar de baja este mix porque tiene " +
                    redondear(cantidadPendiente) + " kg pendientes de despacho en pedidos activos.");
        }
    }

    // Recalcula el costo por kg del mix:
    //   costo fórmula = (Σ gramos_materiaPrima / 1000 * precio) / cantidad que produce la fórmula  → costo MP por kg
    //   costo adicional por kg = Σ (costos adicionales que aplican a la presentación del mix) / cantidadPorUnidad
    //   costo mix = costo MP por kg + costo adicional por kg
    // Cada costo adicional tiene una presentación (1 kg, 5 kg o todos) y su valor
    // es por UNIDAD de esa presentación (una bolsa de 5 kg no vale lo mismo que una de 1 kg).
    // Se mantiene todo por kg internamente (el stock y las estadísticas trabajan en kg).
    @Transactional
    public void recalcularCosto(Long mixId) {

        Mix mix = repository.findById(mixId).orElse(null);
        if (mix == null) {
            return;
        }

        double costoFormula = 0;
        Formula formula = formulaRepository.findByMixId(mixId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);
        if (formula != null && formula.getCantidad() > 0) {
            costoFormula = formula.getCosto();
        }

        double cantidadPorUnidad = mix.getCantidadPorUnidadOrDefault();
        PresentacionCosto objetivo = presentacionParaUnidad(cantidadPorUnidad);

        double costoAdicional = costoAdicionalRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getEliminado()))
                .filter(c -> aplicaPresentacion(c.getPresentacionOrDefault(), objetivo))
                .mapToDouble(c -> cantidadPorUnidad > 0 ? c.getValor() / cantidadPorUnidad : c.getValor())
                .sum();

        double costoPorKg = costoFormula + costoAdicional;
        mix.setCosto(redondear(costoPorKg));
        repository.save(mix);
    }

    // Desglosa el costo por kg de un mix en: costo de materia prima (fórmula) +
    // costos adicionales aplicables a la presentación (cada uno es por paquete).
    @Transactional(readOnly = true)
    public DesgloseCostoMixDTO desgloseCostos(Long mixId) {
        Mix mix = repository.findById(mixId).orElse(null);
        DesgloseCostoMixDTO dto = new DesgloseCostoMixDTO();
        if (mix == null) {
            return dto;
        }

        double cantidadPorUnidad = mix.getCantidadPorUnidadOrDefault();
        dto.setCantidadPorUnidad(cantidadPorUnidad);

        // 1) Fórmula: detalle por materia prima y costo MP por kg
        DesgloseCostoMixDTO.FormulaDesglose formulaDto = new DesgloseCostoMixDTO.FormulaDesglose();
        Formula formula = formulaRepository.findByMixId(mixId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);

        if (formula != null) {
            formulaDto.setCantidad(formula.getCantidad());
            double subtotal = 0;
            List<DetalleFormula> detalles = formula.getDetalles() != null ? formula.getDetalles() : new ArrayList<>();
            for (DetalleFormula detalle : detalles) {
                if (detalle.getMateriaPrima() == null) {
                    continue;
                }
                double gramos = detalle.getGramos();
                double precioPorKg = detalle.getMateriaPrima().getPrecio();
                double costo = (gramos / 1000.0) * precioPorKg;
                subtotal += costo;

                DesgloseCostoMixDTO.DetalleDesglose detDto = new DesgloseCostoMixDTO.DetalleDesglose();
                detDto.setMateriaPrima(detalle.getMateriaPrima().getNombre());
                detDto.setGramos(gramos);
                detDto.setPrecioPorKg(precioPorKg);
                detDto.setCosto(costo);
                formulaDto.getDetalles().add(detDto);
            }
            formulaDto.setSubtotalMateriaPrima(subtotal);
            double costoPorKg = formula.getCantidad() > 0 ? subtotal / formula.getCantidad() : 0;
            formulaDto.setCostoPorKg(costoPorKg);
        }
        dto.setFormula(formulaDto);
        dto.setCostoMateriaPrimaPorKg(formulaDto.getCostoPorKg());

        // 2) Costos adicionales que aplican a la presentación del mix
        PresentacionCosto objetivo = presentacionParaUnidad(cantidadPorUnidad);
        double totalAdicionalPorKg = 0;
        List<CostoAdicional> costos = costoAdicionalRepository.findAll();
        for (CostoAdicional c : costos) {
            if (Boolean.TRUE.equals(c.getEliminado())) {
                continue;
            }
            PresentacionCosto presentacion = c.getPresentacionOrDefault();
            if (!aplicaPresentacion(presentacion, objetivo)) {
                continue;
            }
            double valorPorPaquete = redondear(c.getValor());
            double aportePorKg = cantidadPorUnidad > 0 ? redondear(c.getValor() / cantidadPorUnidad) : redondear(c.getValor());

            DesgloseCostoMixDTO.AdicionalDesglose adv = new DesgloseCostoMixDTO.AdicionalDesglose();
            adv.setDescripcion(c.getDescripcion());
            adv.setPresentacion(presentacion != null ? presentacion.getEtiqueta() : "Todos");
            adv.setValorPorPaquete(valorPorPaquete);
            adv.setAportePorKg(aportePorKg);
            dto.getAdicionales().add(adv);

            totalAdicionalPorKg += aportePorKg;
        }
        dto.setCostosAdicionalesPorKg(redondear(totalAdicionalPorKg));
        dto.setCostoFinalPorKg(redondear(dto.getCostoMateriaPrimaPorKg() + totalAdicionalPorKg));

        return dto;
    }

    // Determina la presentación (1 kg / 5 kg) del mix según su cantidad por unidad.
    // Si el mix no tiene una presentación reconocida, devuelve null (solo aplican costos "Todos").
    private PresentacionCosto presentacionParaUnidad(double cantidadPorUnidad) {
        if (Math.abs(cantidadPorUnidad - 1.0) < 0.05) {
            return PresentacionCosto.UNO_KG;
        }
        if (Math.abs(cantidadPorUnidad - 5.0) < 0.05) {
            return PresentacionCosto.CINCO_KG;
        }
        return null;
    }

    // Un costo adicional aplica si es para "todos" o si coincide con la presentación del mix.
    private boolean aplicaPresentacion(PresentacionCosto presentacion, PresentacionCosto objetivo) {
        if (presentacion == PresentacionCosto.TODOS) {
            return true;
        }
        return objetivo != null && presentacion == objetivo;
    }

    @Transactional
    public void recalcularTodosLosCostos() {
        repository.findAll().stream()
                .filter(m -> !Boolean.TRUE.equals(m.getEliminado()))
                .forEach(m -> recalcularCosto(m.getId()));
    }

    /**
     * Actualiza el stock al registrar una elaboración:
     *  - valida que el mix tenga fórmula y que haya stock de materia prima suficiente
     *  - descuenta de cada materia prima lo necesario según la fórmula (managed → dirty checking)
     *  - suma la cantidad elaborada al stock del mix (detached → save hace merge)
     * Sin desglose de lotes: reparte en FEFO automáticamente (compatibilidad).
     */
    @Transactional
    public void actualizarStockMixElaboracion(Mix mix, Double cantidad) throws ErrorServiceException {
        actualizarStockMixElaboracion(mix, cantidad, null, null);
    }

    /**
     * Igual que el anterior, pero permite elegir manualmente qué lotes consumir
     * (modo híbrido). Si llega usoLotes con entradas válidas para una materia
     * prima, se valida y se respeta el reparto del operario; si no, se usa FEFO.
     * Cuando se provee el LoteMix, se registra el desglose DetalleConsumoLote
     * (sumando entre tandas del mismo lote del día).
     */
    @Transactional
    public void actualizarStockMixElaboracion(Mix mix, Double cantidad, LoteMix loteMix,
                                              Map<Long, Double> usoLotes) throws ErrorServiceException {
        if (mix == null || mix.getId() == null) {
            throw new ErrorServiceException("Debe indicar el mix elaborado.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }

        Formula formula = formulaRepository.findByMixId(mix.getId()).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);

        if (formula == null) {
            throw new ErrorServiceException("El mix no tiene una fórmula asociada. Registre la fórmula antes de elaborar.");
        }
        if (formula.getCantidad() <= 0) {
            throw new ErrorServiceException("La fórmula del mix no tiene un rendimiento válido.");
        }

        // 1) Validar stock de todas las materias primas ANTES de descontar nada
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (detalle.getMateriaPrima() == null || detalle.getGramos() <= 0) {
                continue;
            }
            double necesario = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad())) * cantidad);
            double disponible = detalle.getMateriaPrima().getCantidadActual();
            if (disponible < necesario) {
                throw new ErrorServiceException(
                        "Stock insuficiente de '" + detalle.getMateriaPrima().getNombre()
                        + "': se necesitan " + necesario + " kg y hay " + disponible + " kg.");
            }
        }

        // 2) Descontar de cada materia prima: stock global + desglose por lote
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (detalle.getMateriaPrima() == null || detalle.getGramos() <= 0) {
                continue;
            }
            double necesario = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad())) * cantidad);
            MateriaPrima mp = materiaPrimaRepository.findById(detalle.getMateriaPrima().getId())
                    .orElse(detalle.getMateriaPrima());

            Map<Long, Double> split = null;
            if (usoLotes != null && !usoLotes.isEmpty()) {
                split = evaluarSplitManual(mp, necesario, usoLotes);
            }
            if (split == null) {
                split = splitFEFO(mp, necesario);
            }

            mp.actualizarStock(-necesario);
            materiaPrimaRepository.save(mp);

            for (Map.Entry<Long, Double> e : split.entrySet()) {
                DetalleIngresoMP lote = detalleIngresoMPRepository.findById(e.getKey()).orElse(null);
                if (lote == null) {
                    continue;
                }
                lote.setCantidadRestante(redondear(lote.getRestante() - e.getValue()));
                detalleIngresoMPRepository.save(lote);
            }

            if (loteMix != null) {
                registrarConsumos(loteMix, mp, split);
            }
        }

        // 3) Aumentar el stock del mix
        Mix mixEntidad = repository.findById(mix.getId()).orElse(mix);
        mixEntidad.actualizarStock(cantidad);
        repository.save(mixEntidad);
    }

    /**
     * Desglose propuesto (FEFO) para la vista previa del formulario de
     * elaboración: por cada materia prima de la fórmula indica lo necesario y
     * los lotes disponibles con la cantidad sugerida (ajustable por el operario).
     */
    @Transactional(readOnly = true)
    public DesgloseElaboracionDTO desgloseFEFO(Mix mix, Double cantidad) throws ErrorServiceException {
        if (mix == null || mix.getId() == null) {
            throw new ErrorServiceException("Debe seleccionar el mix a elaborar.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }

        Formula formula = formulaRepository.findByMixId(mix.getId()).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);

        if (formula == null) {
            throw new ErrorServiceException("El mix no tiene una fórmula asociada. Registre la fórmula antes de elaborar.");
        }
        if (formula.getCantidad() <= 0) {
            throw new ErrorServiceException("La fórmula del mix no tiene un rendimiento válido.");
        }

        List<DesgloseElaboracionDTO.ItemMateriaPrima> items = new ArrayList<>();
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (detalle.getMateriaPrima() == null || detalle.getGramos() <= 0) {
                continue;
            }
            double necesario = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad())) * cantidad);
            MateriaPrima mp = detalle.getMateriaPrima();

            Map<Long, Double> split = splitFEFO(mp, necesario);
            String unidad = mp.getUnidadMedida() != null && mp.getUnidadMedida().getDescripcion() != null
                    ? mp.getUnidadMedida().getDescripcion() : "kg";

            List<DesgloseElaboracionDTO.LoteSugerido> lotes = new ArrayList<>();
            for (DetalleIngresoMP lote : detalleIngresoMPRepository.findLotesDisponiblesFEFO(mp.getId())) {
                if (lote.getRestante() <= 0) {
                    continue;
                }
                lotes.add(new DesgloseElaboracionDTO.LoteSugerido(
                        lote.getId(),
                        lote.getNumeroLote(),
                        lote.getFechaVencimiento(),
                        lote.getRestante(),
                        split.getOrDefault(lote.getId(), 0.0)));
            }

            items.add(new DesgloseElaboracionDTO.ItemMateriaPrima(
                    mp.getId(), mp.getNombre(), unidad, necesario, lotes));
        }

        if (items.isEmpty()) {
            throw new ErrorServiceException("La fórmula del mix no tiene detalles de materia prima.");
        }

        return new DesgloseElaboracionDTO(mix.getId(), mix.getNombre(), cantidad, items);
    }

    /**
     * Desglose para el formulario de EDICIÓN de una elaboración: propone el
     * reparto ya registrado (ajustable) y, para cada lote, muestra como
     * "disponible" lo que quedará una vez revertido el consumo actual, de modo
     * que el operario pueda redistribuir entre lotes (incluso más de uno si un
     * solo lote no alcanza) sin que el preview marque stock insuficiente.
     */
    @Transactional(readOnly = true)
    public DesgloseElaboracionDTO desgloseFEFOEdicion(Mix mix, Double cantidad, LoteMix loteMix) throws ErrorServiceException {
        if (mix == null || mix.getId() == null) {
            throw new ErrorServiceException("Debe seleccionar el mix a elaborar.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }

        Formula formula = formulaRepository.findByMixId(mix.getId()).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);

        if (formula == null) {
            throw new ErrorServiceException("El mix no tiene una fórmula asociada. Registre la fórmula antes de elaborar.");
        }
        if (formula.getCantidad() <= 0) {
            throw new ErrorServiceException("La fórmula del mix no tiene un rendimiento válido.");
        }

        // Consumo ya registrado de esta elaboración, agrupado por materia prima y lote.
        Map<Long, Map<Long, Double>> consumosActuales = new HashMap<>();
        if (loteMix != null && loteMix.getId() != null) {
            for (DetalleConsumoLote c : consumoLoteRepository.findByLoteMixId(loteMix.getId())) {
                if (c.getMateriaPrima() == null || c.getLote() == null
                        || c.getMateriaPrima().getId() == null || c.getLote().getId() == null) {
                    continue;
                }
                consumosActuales
                        .computeIfAbsent(c.getMateriaPrima().getId(), k -> new HashMap<>())
                        .merge(c.getLote().getId(), c.getCantidadConsumida(), Double::sum);
            }
        }

        List<DesgloseElaboracionDTO.ItemMateriaPrima> items = new ArrayList<>();
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (detalle.getMateriaPrima() == null || detalle.getGramos() <= 0) {
                continue;
            }
            double necesario = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad())) * cantidad);
            MateriaPrima mp = detalle.getMateriaPrima();

            Map<Long, Double> usadosMP = consumosActuales.getOrDefault(mp.getId(), Map.of());
            boolean hayConsumoRegistrado = usadosMP.values().stream().anyMatch(v -> v > 0);
            Map<Long, Double> split = splitFEFO(mp, necesario);
            String unidad = mp.getUnidadMedida() != null && mp.getUnidadMedida().getDescripcion() != null
                    ? mp.getUnidadMedida().getDescripcion() : "kg";

            List<DesgloseElaboracionDTO.LoteSugerido> lotes = new ArrayList<>();
            for (DetalleIngresoMP lote : detalleIngresoMPRepository.findLotesDisponiblesFEFO(mp.getId())) {
                double usado = redondear(usadosMP.getOrDefault(lote.getId(), 0.0));
                // En edición, lo "disponible" = restante actual + lo que este
                // desglose revierte al guardar, así el preview admite re-partir.
                double disponible = redondear(lote.getRestante() + (hayConsumoRegistrado ? usado : 0.0));
                if (disponible <= 0) {
                    continue;
                }
                double sugerido = hayConsumoRegistrado ? usado : split.getOrDefault(lote.getId(), 0.0);
                lotes.add(new DesgloseElaboracionDTO.LoteSugerido(
                        lote.getId(),
                        lote.getNumeroLote(),
                        lote.getFechaVencimiento(),
                        disponible,
                        sugerido));
            }

            items.add(new DesgloseElaboracionDTO.ItemMateriaPrima(
                    mp.getId(), mp.getNombre(), unidad, necesario, lotes));
        }

        if (items.isEmpty()) {
            throw new ErrorServiceException("La fórmula del mix no tiene detalles de materia prima.");
        }

        return new DesgloseElaboracionDTO(mix.getId(), mix.getNombre(), cantidad, items);
    }

    // Descuenta la cantidad necesaria de los lotes de la materia prima en orden FEFO
    // (vence antes primero). Los lotes están dentro de la transacción → dirty checking.
    private void consumirLotesFEFO(MateriaPrima materiaPrima, double necesario) {
        List<DetalleIngresoMP> lotes = detalleIngresoMPRepository.findLotesDisponiblesFEFO(materiaPrima.getId());
        double pendiente = necesario;
        for (DetalleIngresoMP lote : lotes) {
            if (pendiente <= 0) {
                break;
            }
            double restante = lote.getRestante();
            if (restante <= 0) {
                continue;
            }
            double aDescontar = Math.min(restante, pendiente);
            lote.setCantidadRestante(restante - aDescontar);
            detalleIngresoMPRepository.save(lote);
            pendiente -= aDescontar;
        }
    }

    // Reparte lo necesario entre los lotes disponibles en FEFO, devolviendo
    // un mapa loteId → cantidad. No descuenta nada.
    private Map<Long, Double> splitFEFO(MateriaPrima mp, double necesario) {
        Map<Long, Double> split = new LinkedHashMap<>();
        double pendiente = necesario;
        for (DetalleIngresoMP lote : detalleIngresoMPRepository.findLotesDisponiblesFEFO(mp.getId())) {
            if (pendiente <= 0) {
                break;
            }
            double restante = lote.getRestante();
            if (restante <= 0) {
                continue;
            }
            double aConsumir = Math.min(restante, pendiente);
            if (aConsumir > 0) {
                split.put(lote.getId(), redondear(aConsumir));
            }
            pendiente -= aConsumir;
        }
        return split;
    }

    // Valida el reparto manual del operario para una materia prima.
    // Devuelve null si no hay lotes intervenidos para esa MP (→ se usa FEFO).
    private Map<Long, Double> evaluarSplitManual(MateriaPrima mp, double necesario,
                                                 Map<Long, Double> usoLotes) throws ErrorServiceException {
        Map<Long, Double> candidato = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> e : usoLotes.entrySet()) {
            Long loteId = e.getKey();
            Double cantidad = e.getValue();
            if (loteId == null || cantidad == null || cantidad <= 0) {
                continue;
            }
            DetalleIngresoMP lote = detalleIngresoMPRepository.findById(loteId).orElse(null);
            if (lote == null || Boolean.TRUE.equals(lote.getEliminado())) {
                continue;
            }
            if (!mp.getId().equals(lote.getMateriaPrima().getId())) {
                continue;
            }
            candidato.put(loteId, cantidad);
        }

        if (candidato.isEmpty()) {
            return null;
        }

        double suma = 0;
        for (Map.Entry<Long, Double> e : candidato.entrySet()) {
            DetalleIngresoMP lote = detalleIngresoMPRepository.findById(e.getKey()).orElse(null);
            if (lote == null) {
                continue;
            }
            double cantidad = e.getValue();
            if (cantidad > lote.getRestante() + 0.000001) {
                throw new ErrorServiceException(
                        "El lote " + etiquetaLote(lote) + " sólo tiene disponible " + lote.getRestante() + " kg.");
            }
            suma += cantidad;
        }

        if (Math.abs(suma - necesario) > 0.000001) {
            throw new ErrorServiceException(
                    "En la materia prima '" + mp.getNombre() + "' el reparto entre lotes suma " + redondear(suma)
                    + " kg y se necesitan " + necesario + " kg.");
        }

        return candidato;
    }

    // Persiste el desglose de consumo; si el mismo lote ya aportó a este LoteMix
    // (segunda tanda del día), se suma en lugar de duplicar la fila.
    private void registrarConsumos(LoteMix loteMix, MateriaPrima mp, Map<Long, Double> split) {
        Map<Long, DetalleConsumoLote> existentes = consumoLoteRepository.findByLoteMixId(loteMix.getId()).stream()
                .filter(c -> c.getLote() != null && c.getLote().getId() != null)
                .collect(Collectors.toMap(c -> c.getLote().getId(), c -> c));

        for (Map.Entry<Long, Double> e : split.entrySet()) {
            DetalleConsumoLote consumo = existentes.get(e.getKey());
            if (consumo != null) {
                consumo.setCantidadConsumida(redondear(consumo.getCantidadConsumida() + e.getValue()));
                consumoLoteRepository.save(consumo);
            } else {
                DetalleIngresoMP lote = detalleIngresoMPRepository.findById(e.getKey()).orElse(null);
                if (lote == null) {
                    continue;
                }
                consumo = DetalleConsumoLote.builder()
                        .loteMix(loteMix)
                        .materiaPrima(mp)
                        .lote(lote)
                        .cantidadConsumida(redondear(e.getValue()))
                        .build();
                consumo.setEliminado(false);
                consumoLoteRepository.save(consumo);
            }
        }
    }

    private String etiquetaLote(DetalleIngresoMP lote) {
        if (lote.getNumeroLote() != null && !lote.getNumeroLote().isEmpty()) {
            return "'" + lote.getNumeroLote() + "'";
        }
        return "#" + lote.getId();
    }

    /**
     * Aplica el consumo FIFO de los lotes de materia prima para una elaboración,
     * sin tocar el stock global de la MP ni el del mix (solo descuenta los lotes).
     * Se usa en el seed de datos para que "Restante Lote" quede coherente con las
     * elaboraciones, replicando lo que hace actualizarStockMixElaboracion.
     */
    @Transactional
    public void consumirLotesPorElaboracion(Long mixId, double cantidad) {
        Formula formula = formulaRepository.findByMixId(mixId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);
        if (formula == null || formula.getCantidad() <= 0 || cantidad <= 0) {
            return;
        }
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (detalle.getMateriaPrima() == null || detalle.getGramos() <= 0) {
                continue;
            }
            double necesario = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad())) * cantidad);
            consumirLotesFEFO(detalle.getMateriaPrima(), necesario);
        }
    }

    // Elimina el ruido del punto flotante: redondea a 6 decimales (0,000001 kg = 1 mg).
    // Conserva cantidades chicas como 0,325 g (= 0,000325 kg) sin dejar 7.000000000000001.
    private static double redondear(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }

    /**
     * Devuelve un mapa mixId → cantidad pendiente de despacho (solo pedidos PENDIENTE).
     * Se usa en el listado de mixes para indicar cuánto falta cubrir.
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> cantidadesPendientesPorMix() {
        List<Object[]> resultados = detallePedidoRepository.sumCantidadPendienteByMixId();
        Map<Long, Double> mapa = new HashMap<>();
        for (Object[] fila : resultados) {
            Long mixId = (Long) fila[0];
            Double total = (Double) fila[1];
            if (mixId != null && total != null && total > 0) {
                mapa.put(mixId, redondear(total));
            }
        }
        return mapa;
    }

    /**
     * Devuelve un mapa mixId → cantidad total PEDIDA (comprometida) en pedidos
     * activos no despachados (PENDIENTE o PREPARADO). Incluye ítems preparados y
     * pendientes. Se muestra en una columna propia del listado de mixes y es
     * independiente del stock (el stock libre ya no incluye lo preparado).
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> cantidadesPedidasPorMix() {
        List<Object[]> resultados = detallePedidoRepository.sumCantidadPedidaByMixId();
        Map<Long, Double> mapa = new HashMap<>();
        for (Object[] fila : resultados) {
            Long mixId = (Long) fila[0];
            Double total = (Double) fila[1];
            if (mixId != null && total != null) {
                mapa.put(mixId, redondear(total));
            }
        }
        return mapa;
    }

    /**
     * Registra un cambio de precio de venta en el historial (HU-8).
     */
    @Transactional
    public void registrarCambioPrecio(Mix mix, Double precioAnterior, Double precioNuevo, Usuario usuario) {
        if (precioAnterior == null && precioNuevo == null) return;
        if (precioAnterior != null && precioNuevo != null && Math.abs(precioAnterior - precioNuevo) < 0.001) return;

        HistorialPrecioMix registro = new HistorialPrecioMix();
        registro.setMix(mix);
        registro.setPrecioAnterior(precioAnterior);
        registro.setPrecioNuevo(precioNuevo);
        registro.setFechaHora(java.time.LocalDateTime.now());
        registro.setUsuario(usuario);
        registro.setEliminado(false);
        historialPrecioRepository.save(registro);
    }

    /**
     * Obtiene el historial de precios de un mix.
     */
    public List<HistorialPrecioMix> obtenerHistorialPrecio(Long mixId) {
        return historialPrecioRepository.findByMixIdOrderByFechaHoraDesc(mixId);
    }
}
