package com.primeraPulpa.Services;

import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pedido es la entidad más compleja del ABM: tiene 3 dependencias extra
 * (DetallePedidoRepository, MixRepository, HistorialEstadoPedidoRepository) y
 * su propia lógica de estados (PENDIENTE → PREPARADO/CANCELADO → ...), pero
 * los 7 tests heredados siguen aplicando porque siguen usando el alta()/
 * modificar()/bajaLogica() genéricos de BaseService
 * Acá se agregan tests de las 3 validaciones propias de validar(), y uno de
 * la regla de transición de estados (HU-14): un pedido ENTREGADO no admite
 * más cambios de estado.
 */
class PedidoServiceTest extends AbstractBaseServiceTest<Pedido, Long> {

    private DetallePedidoRepository detallePedidoRepository;
    private HistorialEstadoPedidoRepository historialRepository;

    @Override
    protected BaseRepository<Pedido, Long> crearRepositoryMock() {
        return mock(PedidoRepository.class);
    }

    @Override
    protected BaseService<Pedido, Long> crearService(BaseRepository<Pedido, Long> repository) {
        detallePedidoRepository = mock(DetallePedidoRepository.class);
        MixRepository mixRepository = mock(MixRepository.class);
        historialRepository = mock(HistorialEstadoPedidoRepository.class);
        return new PedidoService((PedidoRepository) repository, detallePedidoRepository,
                mixRepository, historialRepository);
    }

    @Override
    protected Pedido crearEntidadValida() {
        Pedido pedido = new Pedido();
        pedido.setCliente(new Cliente());
        pedido.setUsuario(new Usuario());
        DetallePedido detalle = DetallePedido.builder()
                .mix(new Mix())
                .cantidad(10)
                .build();
        pedido.setDetalles(List.of(detalle));
        return pedido;
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    @Test
    void alta_fallaSiNoTieneCliente() {
        Pedido invalido = new Pedido();
        invalido.setUsuario(new Usuario());
        invalido.setDetalles(List.of(DetallePedido.builder().mix(new Mix()).cantidad(5).build()));

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("cliente del pedido");
    }

    @Test
    void alta_fallaSiNoTieneUsuario() {
        Pedido invalido = new Pedido();
        invalido.setCliente(new Cliente());
        invalido.setDetalles(List.of(DetallePedido.builder().mix(new Mix()).cantidad(5).build()));

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("usuario que registra");
    }

    @Test
    void alta_fallaSiNoTieneDetalles() {
        Pedido invalido = new Pedido();
        invalido.setCliente(new Cliente());
        invalido.setUsuario(new Usuario());
        invalido.setDetalles(List.of());

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("al menos un mix");
    }

    @Test
    void cambiarEstado_fallaSiElPedidoYaFueEntregado() {
        Long id = idDeEjemplo();
        Pedido entregado = crearEntidadValida();
        entregado.setId(id);
        entregado.setEstadoPedido(EstadoPedido.ENTREGADO);

        when(repository.findById(id)).thenReturn(Optional.of(entregado));

        PedidoService pedidoService = (PedidoService) service;

        assertThatThrownBy(() -> pedidoService.cambiarEstado(id, "CANCELADO", new Usuario()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No se puede cambiar el estado");
    }
}