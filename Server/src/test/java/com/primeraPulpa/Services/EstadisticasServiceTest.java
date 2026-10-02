package com.primeraPulpa.Services;

import com.primeraPulpa.dto.DetalleEstadisticaMesDTO;
import com.primeraPulpa.dto.EstadisticaAnualDTO;
import com.primeraPulpa.dto.EstadisticaMensualDTO;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.repositories.IngresoMPRepository;
import com.primeraPulpa.repositories.LoteMixRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private LoteMixRepository loteMixRepository;

    @Mock
    private IngresoMPRepository ingresoMPRepository;

    private EstadisticasService estadisticasService;

    @BeforeEach
    void setUp() {
        estadisticasService = new EstadisticasService(pedidoRepository, loteMixRepository, ingresoMPRepository);
    }

    @Test
    void calcularMensuales_devuelveDoceMesesAunqueNoHayaDatos() {
        int anio = 2026;
        when(pedidoRepository.findByFechaBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loteMixRepository.findAllByEliminadoFalseAndFechaElaboracionBetweenOrderByFechaElaboracionAscIdAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(ingresoMPRepository.findAllByEliminadoFalseAndFechaHoraBetweenOrderByFechaHoraAscIdAsc(any(), any()))
                .thenReturn(Collections.emptyList());

        List<EstadisticaMensualDTO> resultado = estadisticasService.calcularMensuales(anio);

        assertThat(resultado).hasSize(12);
        assertThat(resultado.get(0).mes()).isEqualTo(1);
        assertThat(resultado.get(11).mes()).isEqualTo(12);
        assertThat(resultado.get(0).facturado()).isZero();
    }

    @Test
    void desgloseMensualPorMix_calculaKilosFacturadoYCostoCorrectamente() {
        int anio = 2026;
        int mes = 5;

        Mix mix = new Mix();
        mix.setId(1L);
        mix.setNombre("Mix Energético");
        mix.setCosto(2000.0);

        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDate.of(anio, mes, 10));
        pedido.setEstadoPedido(EstadoPedido.ENTREGADO);

        DetallePedido detalle = DetallePedido.builder()
                .pedido(pedido)
                .mix(mix)
                .cantidad(10.0) // 10 kg
                .precioUnitario(3000.0) // 3000 c/u -> Facturado: 30000, Costo: 20000, Ganancia: 10000
                .build();
        pedido.setDetalles(List.of(detalle));

        when(pedidoRepository.findByFechaBetween(any(), any())).thenReturn(List.of(pedido));

        List<DetalleEstadisticaMesDTO> desglose = estadisticasService.desgloseMensualPorMix(anio, mes);

        assertThat(desglose).hasSize(1);
        DetalleEstadisticaMesDTO item = desglose.get(0);

        assertThat(item.nombreMix()).isEqualTo("Mix Energético");
        assertThat(item.cantidadVendida()).isEqualTo(10.0);
        assertThat(item.facturado()).isEqualTo(30000.0);
        assertThat(item.costo()).isEqualTo(20000.0);
        assertThat(item.ganancia()).isEqualTo(10000.0);
    }

    @Test
    void desgloseMensualPorMix_noSumaFacturadoSiElPedidoEstaCancelado() {
        int anio = 2026;
        int mes = 5;

        Mix mix = new Mix();
        mix.setNombre("Mix Clásico");
        mix.setCosto(1000.0);

        Pedido pedidoCancelado = new Pedido();
        pedidoCancelado.setFecha(LocalDate.of(anio, mes, 15));
        pedidoCancelado.setEstadoPedido(EstadoPedido.CANCELADO);

        DetallePedido detalle = DetallePedido.builder()
                .pedido(pedidoCancelado)
                .mix(mix)
                .cantidad(5.0)
                .precioUnitario(2500.0)
                .build();
        pedidoCancelado.setDetalles(List.of(detalle));

        when(pedidoRepository.findByFechaBetween(any(), any())).thenReturn(List.of(pedidoCancelado));

        List<DetalleEstadisticaMesDTO> desglose = estadisticasService.desgloseMensualPorMix(anio, mes);

        assertThat(desglose).hasSize(1);
        DetalleEstadisticaMesDTO item = desglose.get(0);
        assertThat(item.cantidadVendida()).isEqualTo(5.0);
        assertThat(item.facturado()).isZero();
    }

    @Test
    void resumenAnual_calculaTotalesRentabilidadYMargen() {
        int anio = 2026;

        // 1. Pedido facturado: 50.000 (10 kg a 5000)
        Mix mix = new Mix();
        mix.setCosto(1500.0);

        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDate.of(anio, 3, 1));
        pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
        DetallePedido detPedido = DetallePedido.builder()
                .pedido(pedido)
                .mix(mix)
                .cantidad(10.0)
                .precioUnitario(5000.0)
                .build();
        pedido.setDetalles(List.of(detPedido));

        // 2. Lote elaborado: 20 kg (Costo prod = 20 * 1500 = 30.000)
        LoteMix lote = LoteMix.builder()
                .mix(mix)
                .fechaElaboracion(LocalDate.of(anio, 4, 1))
                .cantidadElaborada(20.0)
                .build();

        // 3. Ingreso MP: 100 kg ingresados
        IngresoMP ingreso = IngresoMP.builder()
                .fechaHora(LocalDateTime.of(anio, 2, 1, 10, 0))
                .build();
        DetalleIngresoMP detIngreso = DetalleIngresoMP.builder()
                .cantidad(100.0)
                .build();
        ingreso.setDetalles(List.of(detIngreso));

        when(pedidoRepository.findByFechaBetween(any(), any())).thenReturn(List.of(pedido));
        when(loteMixRepository.findAllByEliminadoFalseAndFechaElaboracionBetweenOrderByFechaElaboracionAscIdAsc(any(), any()))
                .thenReturn(List.of(lote));
        when(ingresoMPRepository.findAllByEliminadoFalseAndFechaHoraBetweenOrderByFechaHoraAscIdAsc(any(), any()))
                .thenReturn(List.of(ingreso));

        EstadisticaAnualDTO resumen = estadisticasService.resumenAnual(anio);

        assertThat(resumen.anio()).isEqualTo(anio);
        assertThat(resumen.kgVendidos()).isEqualTo(10.0);
        assertThat(resumen.facturado()).isEqualTo(50000.0);
        assertThat(resumen.kgElaborados()).isEqualTo(20.0);
        assertThat(resumen.costoProduccion()).isEqualTo(30000.0);
        assertThat(resumen.kgIngresados()).isEqualTo(100.0);

        // Rentabilidad = Facturado (50.000) - CostoProduccion (30.000) = 20.000
        assertThat(resumen.rentabilidad()).isEqualTo(20000.0);
        // Margen = (20.000 / 50.000) * 100 = 40%
        assertThat(resumen.margenPorcentaje()).isEqualTo(40.0);
    }
}