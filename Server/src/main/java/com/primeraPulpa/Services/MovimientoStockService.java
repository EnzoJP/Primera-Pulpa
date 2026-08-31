package com.primeraPulpa.Services;

import com.primeraPulpa.dto.MovimientoStockDTO;
import com.primeraPulpa.entities.DetalleFormula;
import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.repositories.DetalleIngresoMPRepository;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.repositories.LoteMixRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.MixRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HU-15 / HU-16: historial de movimientos de StockMP (IngresoMP / ElaboracionMix)
 * y de StockMix (ElaboracionMix / Pedido). Los movimientos se derivan de los
 * registros ya existentes (ingresos, lotes de elaboración y pedidos), que son
 * los que modifican el stock de cada entidad.
 */
@Service
public class MovimientoStockService {

    private final DetalleIngresoMPRepository detalleIngresoMPRepository;
    private final LoteMixRepository loteMixRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final FormulaRepository formulaRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final MixRepository mixRepository;

    public MovimientoStockService(DetalleIngresoMPRepository detalleIngresoMPRepository,
                                  LoteMixRepository loteMixRepository,
                                  DetallePedidoRepository detallePedidoRepository,
                                  FormulaRepository formulaRepository,
                                  MateriaPrimaRepository materiaPrimaRepository,
                                  MixRepository mixRepository) {
        this.detalleIngresoMPRepository = detalleIngresoMPRepository;
        this.loteMixRepository = loteMixRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.formulaRepository = formulaRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.mixRepository = mixRepository;
    }

    /**
     * HU-15: historial de movimientos de una materia prima (StockMP).
     * Entradas: IngresoMP (+). Salidas: ElaboracionMix (−, consumo según fórmula).
     *
     * @return lista de movimientos del período con saldo acumulado y stock actual.
     */
    @Transactional(readOnly = true)
    public ListaMovimientos historialStockMP(Long mpId, LocalDate desde, LocalDate hasta) {
        MateriaPrima mp = materiaPrimaRepository.findById(mpId).orElse(null);
        if (mp == null || Boolean.TRUE.equals(mp.getEliminado())) {
            return ListaMovimientos.vacia();
        }

        List<MovimientoStockDTO> todos = new ArrayList<>();

        // 1) Entradas por Ingreso de Materia Prima
        for (DetalleIngresoMP detalle : detalleIngresoMPRepository.findByMateriaPrimaId(mpId)) {
            LocalDate fecha = detalle.getIngresoMP() != null
                    ? detalle.getIngresoMP().getFechaHora().toLocalDate()
                    : null;
            if (fecha == null) continue;
            String usuario = detalle.getIngresoMP().getUsuario() != null
                    ? detalle.getIngresoMP().getUsuario().getNombre()
                    : null;
            Long docId = detalle.getIngresoMP().getId();
            todos.add(new MovimientoStockDTO("IngresoMateriaPrima", fecha,
                    detalle.getCantidad(), mp.getNombre(), usuario, docId, 0));
        }

        // 2) Salidas por Elaboraciones de mixes que consumen esta materia prima
        for (LoteMix lote : loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc()) {
            if (lote.getFechaElaboracion() == null || lote.getMix() == null) continue;

            Formula formula = formulaRepository.findByMixId(lote.getMix().getId()).stream()
                    .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                    .findFirst()
                    .orElse(null);
            if (formula == null || formula.getCantidad() <= 0) continue;

            for (DetalleFormula detalle : formula.getDetalles()) {
                if (detalle.getMateriaPrima() != null
                        && detalle.getMateriaPrima().getId().equals(mpId)
                        && detalle.getGramos() > 0) {
                    double consumido = redondear((detalle.getGramos() / (1000.0 * formula.getCantidad()))
                            * (lote.getCantidadElaborada() != null ? lote.getCantidadElaborada() : 0.0));
                    if (consumido <= 0) continue;
                    String usuario = lote.getUsuario() != null ? lote.getUsuario().getNombre() : null;
                    todos.add(new MovimientoStockDTO("ElaboracionMix", lote.getFechaElaboracion(),
                            -consumido, lote.getMix().getNombre(), usuario, lote.getId(), 0));
                    break;
                }
            }
        }

        return completar(todos, mp.getCantidadActual(), desde, hasta, mp.getNombre());
    }

    /**
     * HU-16: historial de movimientos de un mix (StockMix).
     * Entradas: ElaboracionMix (+). Salidas: Pedido (−).
     */
    @Transactional(readOnly = true)
    public ListaMovimientos historialStockMix(Long mixId, LocalDate desde, LocalDate hasta) {
        Mix mix = mixRepository.findById(mixId).orElse(null);
        if (mix == null || Boolean.TRUE.equals(mix.getEliminado())) {
            return ListaMovimientos.vacia();
        }

        List<MovimientoStockDTO> todos = new ArrayList<>();

        // 1) Entradas por Elaboración
        for (LoteMix lote : loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc()) {
            if (lote.getFechaElaboracion() != null && lote.getMix() != null
                    && lote.getMix().getId().equals(mixId)) {
                String usuario = lote.getUsuario() != null ? lote.getUsuario().getNombre() : null;
                todos.add(new MovimientoStockDTO("ElaboracionMix", lote.getFechaElaboracion(),
                        lote.getCantidadElaborada() != null ? lote.getCantidadElaborada() : 0.0,
                        mix.getNombre(), usuario, lote.getId(), 0));
            }
        }

        // 2) Salidas por Pedidos (despacho). Se omiten pedidos eliminados.
        for (DetallePedido detalle : detallePedidoRepository.findByMixId(mixId)) {
            if (detalle.getPedido() == null || detalle.getPedido().getFecha() == null
                    || Boolean.TRUE.equals(detalle.getPedido().getEliminado())) continue;
            String usuario = detalle.getPedido().getUsuario() != null
                    ? detalle.getPedido().getUsuario().getNombre()
                    : null;
            todos.add(new MovimientoStockDTO("Pedido", detalle.getPedido().getFecha(),
                    -detalle.getCantidad(), mix.getNombre(), usuario,
                    detalle.getPedido().getId(), 0));
        }

        return completar(todos, mix.getStock(), desde, hasta, mix.getNombre());
    }

    /**
     * Calcula el saldo acumulado en orden cronológico (viejo → nuevo) y luego
     * muestra los movimientos de lo más nuevo a lo más antiguo. El saldo de cada
     * movimiento se computa recorriendo el historial hacia adelante (depende del
     * orden de los registros en el tiempo), por eso la lista de salida se invierte
     * al final, sobre la lista ya filtrada por período.
     */
    private ListaMovimientos completar(List<MovimientoStockDTO> todos, double stockActual,
                                       LocalDate desde, LocalDate hasta, String nombre) {
        todos.sort(Comparator.comparing(MovimientoStockDTO::getFecha)
                .thenComparing(MovimientoStockDTO::getDocumentoId));

        List<MovimientoStockDTO> conSaldo = new ArrayList<>(todos.size());
        double acumulado = 0;
        for (MovimientoStockDTO m : todos) {
            acumulado += m.getCantidad();
            double saldo = redondear(acumulado);
            conSaldo.add(new MovimientoStockDTO(m.getTipo(), m.getFecha(), m.getCantidad(),
                    m.getDescripcion(), m.getUsuario(), m.getDocumentoId(), saldo));
        }

        List<MovimientoStockDTO> enPeriodo = conSaldo.stream()
                .filter(m -> (desde == null || !m.getFecha().isBefore(desde))
                        && (hasta == null || !m.getFecha().isAfter(hasta)))
                .collect(Collectors.toCollection(ArrayList::new));

        // Mostrar de lo más nuevo a lo más antiguo (el saldo ya se calculó cronológico)
        Collections.reverse(enPeriodo);

        double saldoFinal = 0;
        for (MovimientoStockDTO m : conSaldo) {
            if (hasta == null || !m.getFecha().isAfter(hasta)) {
                saldoFinal = m.getSaldo();
            }
        }

        return new ListaMovimientos(enPeriodo, stockActual, saldoFinal, nombre);
    }

    private static double redondear(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }

    /**
     * Resultado del historial: movimientos del período, stock actual de la entidad
     * y saldo resultante al final del período consultado.
     */
    public static class ListaMovimientos {
        private final List<MovimientoStockDTO> movimientos;
        private final double stockActual;
        private final double saldoFinal;
        private final String nombre;

        public ListaMovimientos(List<MovimientoStockDTO> movimientos, double stockActual,
                                double saldoFinal, String nombre) {
            this.movimientos = movimientos;
            this.stockActual = stockActual;
            this.saldoFinal = saldoFinal;
            this.nombre = nombre;
        }

        public static ListaMovimientos vacia() {
            return new ListaMovimientos(List.of(), 0, 0, null);
        }

        public List<MovimientoStockDTO> getMovimientos() { return movimientos; }
        public double getStockActual() { return stockActual; }
        public double getSaldoFinal() { return saldoFinal; }
        public String getNombre() { return nombre; }
    }
}

