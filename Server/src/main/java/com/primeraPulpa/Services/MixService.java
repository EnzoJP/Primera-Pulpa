package com.primeraPulpa.Services;

import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MixService extends BaseService<Mix, Long> {

    private final DetallePedidoRepository detallePedidoRepository;
    private final FormulaRepository formulaRepository;
    private final CostoAdicionalRepository costoAdicionalRepository;
    private final DetalleIngresoMPRepository detalleIngresoMPRepository;
    private final HistorialPrecioMixRepository historialPrecioRepository;

    public MixService(MixRepository repository, DetallePedidoRepository detallePedidoRepository,
                      FormulaRepository formulaRepository, CostoAdicionalRepository costoAdicionalRepository,
                      DetalleIngresoMPRepository detalleIngresoMPRepository,
                      HistorialPrecioMixRepository historialPrecioRepository) {
        super(repository);
        this.detallePedidoRepository = detallePedidoRepository;
        this.formulaRepository = formulaRepository;
        this.costoAdicionalRepository = costoAdicionalRepository;
        this.detalleIngresoMPRepository = detalleIngresoMPRepository;
        this.historialPrecioRepository = historialPrecioRepository;
    }

    @Override
    protected void validar(Mix mix) throws ErrorServiceException {
        if (mix.getNombre() == null || mix.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del mix");
        }
        if (mix.getPrecioVenta() != null && mix.getPrecioVenta() < 0) {
            throw new ErrorServiceException("El precio de venta no puede ser negativo");
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
    //   costo fórmula = (Σ gramos_materiaPrima / 1000 * precio) / cantidad que produce la fórmula
    //   costo mix = costo fórmula + Σ valores de costos adicionales (bolsa, etiqueta...)
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
        /*if (formula != null && formula.getCantidad() > 0) {
            double costoTotal = formula.getDetalles().stream()
                    .filter(d -> d.getMateriaPrima() != null && d.getGramos() > 0)
                    .mapToDouble(d -> (d.getGramos() / 1000.0) * d.getMateriaPrima().getPrecio())
                    .sum();
            costoFormula = costoTotal / formula.getCantidad();
            }*/


        double costoAdicional = costoAdicionalRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getEliminado()))
                .mapToDouble(CostoAdicional::getValor)
                .sum();

        mix.setCosto(costoFormula + costoAdicional);
        repository.save(mix);
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
            MateriaPrima materiaPrima = detalle.getMateriaPrima();
            materiaPrima.actualizarStock(-necesario);
            consumirLotesFEFO(materiaPrima, necesario);
        }

        // 3) Aumentar el stock del mix (el mix llega detached → save hace merge)
        mix.actualizarStock(cantidad);
        repository.save(mix);
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
            pendiente -= aDescontar;
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
