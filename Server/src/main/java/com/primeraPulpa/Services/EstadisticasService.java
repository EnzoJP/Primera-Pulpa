package com.primeraPulpa.Services;

import com.primeraPulpa.dto.DetalleEstadisticaMesDTO;
import com.primeraPulpa.dto.EstadisticaAnualDTO;
import com.primeraPulpa.dto.EstadisticaMensualDTO;
import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.entities.IngresoMP;
import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.repositories.IngresoMPRepository;
import com.primeraPulpa.repositories.LoteMixRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EstadisticasService {

    private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.of("es", "ES"));

    private final PedidoRepository pedidoRepository;
    private final LoteMixRepository loteMixRepository;
    private final IngresoMPRepository ingresoMPRepository;

    public EstadisticasService(PedidoRepository pedidoRepository,
                               LoteMixRepository loteMixRepository,
                               IngresoMPRepository ingresoMPRepository) {
        this.pedidoRepository = pedidoRepository;
        this.loteMixRepository = loteMixRepository;
        this.ingresoMPRepository = ingresoMPRepository;
    }

    public List<EstadisticaMensualDTO> calcularMensuales(int anio) {
        YearMonth inicio = YearMonth.of(anio, 1);
        YearMonth fin = YearMonth.of(anio, 12);

        LocalDate desde = inicio.atDay(1);
        LocalDate hasta = fin.atEndOfMonth();

        List<Pedido> pedidos = pedidoRepository.findByFechaBetween(desde, hasta);
        List<LoteMix> lotes = loteMixRepository
                .findAllByEliminadoFalseAndFechaElaboracionBetweenOrderByFechaElaboracionAscIdAsc(desde, hasta);
        List<IngresoMP> ingresos = ingresoMPRepository
                .findAllByEliminadoFalseAndFechaHoraBetweenOrderByFechaHoraAscIdAsc(
                        desde.atStartOfDay(), hasta.atTime(23, 59, 59));

        List<EstadisticaMensualDTO> resultado = new ArrayList<>();
        YearMonth cursor = inicio;
        while (!cursor.isAfter(fin)) {
            resultado.add(armarMes(cursor, pedidos, lotes, ingresos));
            cursor = cursor.plusMonths(1);
        }
        return resultado;
    }

    // ── Desglose mensual por mix ─────────────────────────────────────────────
    public List<DetalleEstadisticaMesDTO> desgloseMensualPorMix(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        List<Pedido> pedidos = pedidoRepository.findByFechaBetween(desde, hasta);

        Map<String, double[]> acumulado = new LinkedHashMap<>();
        for (Pedido p : pedidos) {
            if (p.getFecha() == null || !mismoMes(p.getFecha(), periodo)) {
                continue;
            }
            boolean cancelado = p.getEstadoPedido() != null
                    && "CANCELADO".equalsIgnoreCase(p.getEstadoPedido().getDescripcion());
            for (DetallePedido d : p.getDetalles()) {
                if (d == null || d.getMix() == null) {
                    continue;
                }
                Mix mix = d.getMix();
                double cantidad = d.getCantidad();
                double precioVenta = cancelado ? 0 : d.getPrecioUnitario();

                double[] acc = acumulado.computeIfAbsent(mix.getNombre(), k -> new double[3]);
                acc[0] += cantidad;                       // kg vendidos
                acc[1] += cantidad * precioVenta;         // facturado
                acc[2] += cantidad * (precioVenta - mix.getCosto()); // ganancia real
            }
        }

        List<DetalleEstadisticaMesDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, double[]> e : acumulado.entrySet()) {
            double[] acc = e.getValue();
            double costo = acc[1] - acc[2]; // facturado - ganancia
            resultado.add(new DetalleEstadisticaMesDTO(
                    e.getKey(), acc[0], acc[1], costo, acc[2]));
        }
        return resultado;
    }

    // ── Desglose anual por mix ───────────────────────────────────────────────
    public List<DetalleEstadisticaMesDTO> desgloseAnualPorMix(int anio) {
        LocalDate desde = Year.of(anio).atDay(1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);

        List<Pedido> pedidos = pedidoRepository.findByFechaBetween(desde, hasta);

        Map<String, double[]> acumulado = new LinkedHashMap<>();
        for (Pedido p : pedidos) {
            if (p.getFecha() == null || p.getFecha().getYear() != anio) {
                continue;
            }
            boolean cancelado = p.getEstadoPedido() != null
                    && "CANCELADO".equalsIgnoreCase(p.getEstadoPedido().getDescripcion());
            for (DetallePedido d : p.getDetalles()) {
                if (d == null || d.getMix() == null) {
                    continue;
                }
                Mix mix = d.getMix();
                double cantidad = d.getCantidad();
                double precioVenta = cancelado ? 0 : d.getPrecioUnitario();

                double[] acc = acumulado.computeIfAbsent(mix.getNombre(), k -> new double[3]);
                acc[0] += cantidad;
                acc[1] += cantidad * precioVenta;
                acc[2] += cantidad * (precioVenta - mix.getCosto());
            }
        }

        List<DetalleEstadisticaMesDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, double[]> e : acumulado.entrySet()) {
            double[] acc = e.getValue();
            double costo = acc[1] - acc[2];
            resultado.add(new DetalleEstadisticaMesDTO(
                    e.getKey(), acc[0], acc[1], costo, acc[2]));
        }
        return resultado;
    }

    // ── Resumen anual (todo el año) ──────────────────────────────────────────
    public EstadisticaAnualDTO resumenAnual(int anio) {
        LocalDate desde = Year.of(anio).atDay(1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);

        List<Pedido> pedidos = pedidoRepository.findByFechaBetween(desde, hasta);
        List<LoteMix> lotes = loteMixRepository
                .findAllByEliminadoFalseAndFechaElaboracionBetweenOrderByFechaElaboracionAscIdAsc(desde, hasta);
        List<IngresoMP> ingresos = ingresoMPRepository
                .findAllByEliminadoFalseAndFechaHoraBetweenOrderByFechaHoraAscIdAsc(
                        desde.atStartOfDay(), hasta.atTime(23, 59, 59));

        double kgVendidos = 0;
        double facturado = 0;
        double costoProduccion = 0;
        double kgElaborados = 0;
        double kgIngresados = 0;

        for (Pedido p : pedidos) {
            if (p.getFecha() == null || p.getFecha().getYear() != anio) {
                continue;
            }
            boolean cancelado = p.getEstadoPedido() != null
                    && "CANCELADO".equalsIgnoreCase(p.getEstadoPedido().getDescripcion());
            for (DetallePedido d : p.getDetalles()) {
                if (d == null) {
                    continue;
                }
                kgVendidos += d.getCantidad();
                if (!cancelado) {
                    facturado += d.getCantidad() * d.getPrecioUnitario();
                }
            }
        }

        for (LoteMix l : lotes) {
            if (l.getFechaElaboracion() != null && l.getFechaElaboracion().getYear() == anio) {
                double cantidad = l.getCantidadElaborada() != null ? l.getCantidadElaborada() : 0;
                kgElaborados += cantidad;
                if (l.getMix() != null) {
                    costoProduccion += cantidad * l.getMix().getCosto();
                }
            }
        }

        for (IngresoMP ing : ingresos) {
            if (ing.getFechaHora() != null && ing.getFechaHora().getYear() == anio) {
                for (DetalleIngresoMP d : ing.getDetalles()) {
                    if (d != null) {
                        kgIngresados += d.getCantidad();
                    }
                }
            }
        }

        double rentabilidad = facturado - costoProduccion;
        double margen = facturado != 0 ? (rentabilidad / facturado) * 100 : 0;

        return new EstadisticaAnualDTO(anio, kgVendidos, facturado, costoProduccion,
                rentabilidad, margen, kgIngresados, kgElaborados);
    }

    public String nombreMes(int numero) {
        return capitalizar(Month.of(numero).name());
    }

    private EstadisticaMensualDTO armarMes(YearMonth mes,
                                           List<Pedido> pedidos,
                                           List<LoteMix> lotes,
                                           List<IngresoMP> ingresos) {
        double kgVendidos = 0;
        double facturado = 0;
        double costoProduccion = 0;
        double kgElaborados = 0;
        double kgIngresados = 0;

        for (Pedido p : pedidos) {
            if (p.getFecha() == null || !mismoMes(p.getFecha(), mes)) {
                continue;
            }
            boolean cancelado = p.getEstadoPedido() != null
                    && "CANCELADO".equalsIgnoreCase(p.getEstadoPedido().getDescripcion());
            for (DetallePedido d : p.getDetalles()) {
                if (d == null) {
                    continue;
                }
                double cantidad = d.getCantidad();
                kgVendidos += cantidad;
                if (!cancelado) {
                    facturado += cantidad * d.getPrecioUnitario();
                }
            }
        }

        for (LoteMix l : lotes) {
            if (l.getFechaElaboracion() != null && mismoMes(l.getFechaElaboracion(), mes)) {
                double cantidad = l.getCantidadElaborada() != null ? l.getCantidadElaborada() : 0;
                kgElaborados += cantidad;
                if (l.getMix() != null) {
                    costoProduccion += cantidad * l.getMix().getCosto();
                }
            }
        }

        for (IngresoMP ing : ingresos) {
            if (ing.getFechaHora() == null || !mismoMes(ing.getFechaHora(), mes)) {
                continue;
            }
            for (DetalleIngresoMP d : ing.getDetalles()) {
                if (d != null) {
                    kgIngresados += d.getCantidad();
                }
            }
        }

        double rentabilidad = facturado - costoProduccion;
        double margen = facturado != 0 ? (rentabilidad / facturado) * 100 : 0;

        return new EstadisticaMensualDTO(
                mes.getYear(),
                mes.getMonthValue(),
                capitalizar(mes.atDay(1).format(MES_FORMATTER)),
                kgVendidos,
                facturado,
                costoProduccion,
                rentabilidad,
                margen,
                kgIngresados,
                kgElaborados
        );
    }

    private boolean mismoMes(LocalDate fecha, YearMonth mes) {
        return fecha.getYear() == mes.getYear() && fecha.getMonthValue() == mes.getMonthValue();
    }

    private boolean mismoMes(LocalDateTime fechaHora, YearMonth mes) {
        return mismoMes(fechaHora.toLocalDate(), mes);
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
