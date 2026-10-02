package com.primeraPulpa.Services;

import com.primeraPulpa.dto.MovimientoStockDTO;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.repositories.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
@Transactional
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceTest {

    @Mock
    private DetalleIngresoMPRepository detalleIngresoMPRepository;

    @Mock
    private LoteMixRepository loteMixRepository;

    @Mock
    private DetallePedidoRepository detallePedidoRepository;

    @Mock
    private FormulaRepository formulaRepository;

    @Mock
    private MateriaPrimaRepository materiaPrimaRepository;

    @Mock
    private MixRepository mixRepository;

    @Mock
    private DetalleConsumoLoteRepository consumoLoteRepository;

    private MovimientoStockService movimientoStockService;

    @BeforeEach
    void setUp() {
        movimientoStockService = new MovimientoStockService(
                detalleIngresoMPRepository,
                loteMixRepository,
                detallePedidoRepository,
                formulaRepository,
                materiaPrimaRepository,
                mixRepository,
                consumoLoteRepository
        );
    }

    // ── HU-15: Historial Materia Prima ──────────────────────────────────────

    @Test
    void historialStockMP_devuelveVaciaSiNoExisteOEliminada() {
        when(materiaPrimaRepository.findById(1L)).thenReturn(Optional.empty());

        MovimientoStockService.ListaMovimientos resultado =
                movimientoStockService.historialStockMP(1L, null, null);

        assertThat(resultado.getMovimientos()).isEmpty();
        assertThat(resultado.getStockActual()).isZero();

        MateriaPrima eliminada = new MateriaPrima();
        eliminada.setId(2L);
        eliminada.setEliminado(true);
        when(materiaPrimaRepository.findById(2L)).thenReturn(Optional.of(eliminada));

        MovimientoStockService.ListaMovimientos resultadoEliminada =
                movimientoStockService.historialStockMP(2L, null, null);

        assertThat(resultadoEliminada.getMovimientos()).isEmpty();
    }

    @Test
    void historialStockMP_calculaSaldosEntradaYSalidaCorrectamente() {
        Long mpId = 1L;
        MateriaPrima mp = new MateriaPrima();
        mp.setId(mpId);
        mp.setNombre("Almendras");
        mp.setCantidadActual(30.0);
        mp.setEliminado(false);

        when(materiaPrimaRepository.findById(mpId)).thenReturn(Optional.of(mp));

        // 1. Ingreso: +50 kg el 2026-05-01
        IngresoMP ingreso = IngresoMP.builder()
                .fechaHora(LocalDateTime.of(2026, 5, 1, 9, 0))
                .usuario(Usuario.builder().nombre("Recepcionista").build())
                .build();
        ingreso.setId(10L);

        DetalleIngresoMP detIngreso = DetalleIngresoMP.builder()
                .ingresoMP(ingreso)
                .materiaPrima(mp)
                .cantidad(50.0)
                .numeroLote("LOT-1")
                .build();
        when(detalleIngresoMPRepository.findByMateriaPrimaId(mpId)).thenReturn(List.of(detIngreso));

        // 2. Elaboración: Mix con fórmula el 2026-05-05
        // Fórmula produce 10 kg totales usando 4000 gr (4 kg) de Almendras (0.4 kg por kg de Mix)
        Mix mix = new Mix();
        mix.setId(20L);
        mix.setNombre("Mix Energía");

        Formula formula = new Formula();
        formula.setMix(mix);
        formula.setCantidad(10.0);
        formula.setEliminado(false);

        DetalleFormula detFormula = new DetalleFormula();
        detFormula.setMateriaPrima(mp);
        detFormula.setGramos(4000.0);
        formula.setDetalles(List.of(detFormula));

        when(formulaRepository.findByMixId(20L)).thenReturn(List.of(formula));

        // Se elaboran 50 kg de Mix -> consumo = (4000 / (1000 * 10)) * 50 = 0.4 * 50 = 20 kg
        LoteMix lote = LoteMix.builder()
                .mix(mix)
                .fechaElaboracion(LocalDate.of(2026, 5, 5))
                .cantidadElaborada(50.0)
                .usuario(Usuario.builder().nombre("Operario").build())
                .build();
        lote.setId(100L);

        when(loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc())
                .thenReturn(List.of(lote));
        when(consumoLoteRepository.findByLoteMixId(100L)).thenReturn(Collections.emptyList());

        MovimientoStockService.ListaMovimientos res =
                movimientoStockService.historialStockMP(mpId, null, null);

        // La lista final se entrega invertida (más reciente primero)
        assertThat(res.getMovimientos()).hasSize(2);

        MovimientoStockDTO movSalida = res.getMovimientos().get(0);
        assertThat(movSalida.getTipo()).isEqualTo("ElaboracionMix");
        assertThat(movSalida.getCantidad()).isEqualTo(-20.0);
        assertThat(movSalida.getSaldo()).isEqualTo(30.0); // 50 inicial - 20 = 30

        MovimientoStockDTO movEntrada = res.getMovimientos().get(1);
        assertThat(movEntrada.getTipo()).isEqualTo("IngresoMateriaPrima");
        assertThat(movEntrada.getCantidad()).isEqualTo(50.0);
        assertThat(movEntrada.getSaldo()).isEqualTo(50.0);

        assertThat(res.getSaldoFinal()).isEqualTo(30.0);
        assertThat(res.getStockActual()).isEqualTo(30.0);
    }

    // ── HU-16: Historial Mix ────────────────────────────────────────────────

    @Test
    void historialStockMix_devuelveVaciaSiNoExisteOEliminado() {
        when(mixRepository.findById(1L)).thenReturn(Optional.empty());

        MovimientoStockService.ListaMovimientos resultado =
                movimientoStockService.historialStockMix(1L, null, null);

        assertThat(resultado.getMovimientos()).isEmpty();
    }

    @Test
    void historialStockMix_calculaElaboracionYPedidosOmitiendoEliminados() {
        Long mixId = 5L;
        Mix mix = new Mix();
        mix.setId(mixId);
        mix.setNombre("Mix Frutos Secos");
        mix.setStock(15.0);
        mix.setEliminado(false);

        when(mixRepository.findById(mixId)).thenReturn(Optional.of(mix));

        // 1. Entrada: Elaboración de 25 kg el 2026-06-01
        LoteMix lote = LoteMix.builder()
                .mix(mix)
                .fechaElaboracion(LocalDate.of(2026, 6, 1))
                .cantidadElaborada(25.0)
                .usuario(Usuario.builder().nombre("Operario").build())
                .build();
        lote.setId(1L);

        when(loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc())
                .thenReturn(List.of(lote));
        when(consumoLoteRepository.findByLoteMixId(1L)).thenReturn(Collections.emptyList());

        // 2. Salida: Pedido de 10 kg el 2026-06-03
        Pedido pedidoValido = new Pedido();
        pedidoValido.setId(201L);
        pedidoValido.setFecha(LocalDate.of(2026, 6, 3));
        pedidoValido.setEliminado(false);
        pedidoValido.setUsuario(Usuario.builder().nombre("Vendedor").build());

        DetallePedido detValido = DetallePedido.builder()
                .pedido(pedidoValido)
                .mix(mix)
                .cantidad(10.0)
                .build();

        // 3. Salida eliminada: no debe computar
        Pedido pedidoEliminado = new Pedido();
        pedidoEliminado.setId(202L);
        pedidoEliminado.setFecha(LocalDate.of(2026, 6, 4));
        pedidoEliminado.setEliminado(true);

        DetallePedido detEliminado = DetallePedido.builder()
                .pedido(pedidoEliminado)
                .mix(mix)
                .cantidad(5.0)
                .build();

        when(detallePedidoRepository.findByMixId(mixId)).thenReturn(List.of(detValido, detEliminado));

        MovimientoStockService.ListaMovimientos res =
                movimientoStockService.historialStockMix(mixId, null, null);

        assertThat(res.getMovimientos()).hasSize(2);

        // Más reciente primero (Pedido del día 3)
        MovimientoStockDTO movPedido = res.getMovimientos().get(0);
        assertThat(movPedido.getTipo()).isEqualTo("Pedido");
        assertThat(movPedido.getCantidad()).isEqualTo(-10.0);
        assertThat(movPedido.getSaldo()).isEqualTo(15.0); // 25 - 10

        // Elaboración del día 1
        MovimientoStockDTO movElab = res.getMovimientos().get(1);
        assertThat(movElab.getTipo()).isEqualTo("ElaboracionMix");
        assertThat(movElab.getCantidad()).isEqualTo(25.0);
        assertThat(movElab.getSaldo()).isEqualTo(25.0);

        assertThat(res.getSaldoFinal()).isEqualTo(15.0);
    }

    @Test
    void historialStockMix_filtraPorRangoDeFechas() {
        Long mixId = 5L;
        Mix mix = new Mix();
        mix.setId(mixId);
        mix.setNombre("Mix Test");
        mix.setStock(50.0);

        when(mixRepository.findById(mixId)).thenReturn(Optional.of(mix));

        LoteMix loteMayo = LoteMix.builder()
                .mix(mix)
                .fechaElaboracion(LocalDate.of(2026, 5, 10))
                .cantidadElaborada(20.0)
                .build();
        loteMayo.setId(10L);

        LoteMix loteJunio = LoteMix.builder()
                .mix(mix)
                .fechaElaboracion(LocalDate.of(2026, 6, 10))
                .cantidadElaborada(30.0)
                .build();
        loteJunio.setId(20L);

        when(loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc())
                .thenReturn(List.of(loteJunio, loteMayo));
        when(detallePedidoRepository.findByMixId(mixId)).thenReturn(Collections.emptyList());

        // Consultar solo el mes de junio
        LocalDate desde = LocalDate.of(2026, 6, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);

        MovimientoStockService.ListaMovimientos res =
                movimientoStockService.historialStockMix(mixId, desde, hasta);

        assertThat(res.getMovimientos()).hasSize(1);
        assertThat(res.getMovimientos().get(0).getFecha()).isEqualTo(LocalDate.of(2026, 6, 10));
        // El saldo acumula todos los movimientos históricos previos hasta esa fecha (20 + 30 = 50)
        assertThat(res.getMovimientos().get(0).getSaldo()).isEqualTo(50.0);
        assertThat(res.getSaldoFinal()).isEqualTo(50.0);
    }
}