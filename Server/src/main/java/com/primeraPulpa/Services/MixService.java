package com.primeraPulpa.Services;

import com.primeraPulpa.dto.DesgloseCostoMixDTO;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MixService extends BaseService<Mix, Long> {

    private final DetallePedidoRepository detallePedidoRepository;
    private final FormulaRepository formulaRepository;
    private final DetalleIngresoMPRepository detalleIngresoMPRepository;
    private final HistorialPrecioMixRepository historialPrecioRepository;
    private final CostoAdicionalRepository costoAdicionalRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;

    public MixService(MixRepository repository, DetallePedidoRepository detallePedidoRepository,
                      FormulaRepository formulaRepository,
                      DetalleIngresoMPRepository detalleIngresoMPRepository,
                      HistorialPrecioMixRepository historialPrecioRepository,
                      CostoAdicionalRepository costoAdicionalRepository,
                      MateriaPrimaRepository materiaPrimaRepository) {
        super(repository);
        this.detallePedidoRepository = detallePedidoRepository;
        this.formulaRepository = formulaRepository;
        this.detalleIngresoMPRepository = detalleIngresoMPRepository;
        this.historialPrecioRepository = historialPrecioRepository;
        this.costoAdicionalRepository = costoAdicionalRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
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

    // Actualiza el stock al registrar una elaboración:
    //  - valida que el mix tenga fórmula y que haya stock de materia prima suficiente
    //  - descuenta de cada materia prima lo necesario según la fórmula (managed → dirty checking)
    //  - suma la cantidad elaborada al stock del mix (detached → save hace merge)
    @Transactional
    public void actualizarStockMixElaboracion(Mix mix, Double cantidad) throws ErrorServiceException {
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

        // 2) Descontar de cada materia prima: stock global + desglose FIFO por lote
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (detalle.getMateriaPrima() == null || detalle.getGramos() <= 0) {
                continue;
            }
            double necesario = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad())) * cantidad);
            MateriaPrima mp = materiaPrimaRepository.findById(detalle.getMateriaPrima().getId())
                    .orElse(detalle.getMateriaPrima());
            mp.actualizarStock(-necesario);
            materiaPrimaRepository.save(mp);
            consumirLotesFEFO(mp, necesario);
        }

        // 3) Aumentar el stock del mix
        Mix mixEntidad = repository.findById(mix.getId()).orElse(mix);
        mixEntidad.actualizarStock(cantidad);
        repository.save(mixEntidad);
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
