package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DetallePedidoServiceTest extends AbstractBaseServiceTest<DetallePedido, Long> {

    @Override
    protected BaseRepository<DetallePedido, Long> crearRepositoryMock() {
        return mock(DetallePedidoRepository.class);
    }

    @Override
    protected BaseService<DetallePedido, Long> crearService(BaseRepository<DetallePedido, Long> repository) {
        return new DetallePedidoService((DetallePedidoRepository) repository);
    }

    @Override
    protected DetallePedido crearEntidadValida() {
        Pedido pedido = new Pedido();
        pedido.setId(10L);

        Mix mix = new Mix();
        mix.setId(20L);
        mix.setNombre("Mix Clásico Energético");

        return DetallePedido.builder()
                .pedido(pedido)
                .mix(mix)
                .cantidad(5.0)
                .precioUnitario(14500.0)
                .preparado(false)
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Tests de Validaciones de Negocio ---

    @Test
    void alta_fallaSiPedidoEsNulo() {
        DetallePedido detalleSinPedido = crearEntidadValida();
        detalleSinPedido.setPedido(null);

        assertThatThrownBy(() -> service.alta(detalleSinPedido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El detalle debe pertenecer a un pedido");
    }

    @Test
    void alta_fallaSiMixEsNulo() {
        DetallePedido detalleSinMix = crearEntidadValida();
        detalleSinMix.setMix(null);

        assertThatThrownBy(() -> service.alta(detalleSinMix))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar el mix solicitado");
    }

    @Test
    void alta_fallaSiCantidadEsCeroONegativa() {
        DetallePedido detalleCantidadCero = crearEntidadValida();
        detalleCantidadCero.setCantidad(0.0);

        assertThatThrownBy(() -> service.alta(detalleCantidadCero))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La cantidad debe ser mayor a cero");

        DetallePedido detalleCantidadNegativa = crearEntidadValida();
        detalleCantidadNegativa.setCantidad(-2.5);

        assertThatThrownBy(() -> service.alta(detalleCantidadNegativa))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La cantidad debe ser mayor a cero");
    }

    @Test
    void alta_fallaSiPrecioUnitarioEsNegativo() {
        DetallePedido detallePrecioNegativo = crearEntidadValida();
        detallePrecioNegativo.setPrecioUnitario(-100.0);

        assertThatThrownBy(() -> service.alta(detallePrecioNegativo))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El precio unitario no puede ser negativo");
    }
}