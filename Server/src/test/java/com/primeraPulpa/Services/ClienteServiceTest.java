package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.ClienteRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cliente tiene una dependencia extra (PedidoRepository, para la regla de
 * "no eliminar cliente con pedidos") y además SOBRESCRIBE modificar() en vez
 * de usar el de BaseService. Los 7 tests heredados igual aplican porque
 * ClienteService.modificar() respeta el mismo contrato (Optional presente/
 * vacío según exista o no el id).
 */
class ClienteServiceTest extends AbstractBaseServiceTest<Cliente, Long> {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);

    @Override
    protected BaseRepository<Cliente, Long> crearRepositoryMock() {
        return mock(ClienteRepository.class);
    }

    @Override
    protected BaseService<Cliente, Long> crearService(BaseRepository<Cliente, Long> repository) {

        when(pedidoRepository.findByClienteId(any())).thenReturn(List.of());

        return new ClienteService((ClienteRepository) repository, pedidoRepository);
    }

    @Override
    protected Cliente crearEntidadValida() {
        return Cliente.builder()
                .nombre("Almacén Don José")
                .contacto("3614445566")
                .cuit("20-12345678-9")
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    @Test
    void alta_fallaSiNoTieneNombre() {
        Cliente invalido = Cliente.builder().contacto("3614445566").build();

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("nombre del cliente");
    }

    @Test
    void alta_fallaSiNoTieneContacto() {
        Cliente invalido = Cliente.builder().nombre("Almacén Don José").build();

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("contacto del cliente");
    }

    @Test
    void alta_fallaSiElCuitNoTieneSuficientesDigitos() {
        Cliente invalido = Cliente.builder()
                .nombre("Almacén Don José")
                .contacto("3614445566")
                .cuit("--")
                .build();

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("CUIT");
    }

    @Test
    void bajaLogica_fallaSiElClienteTienePedidosAsociados() {
        Long id = idDeEjemplo();
        Cliente existente = crearEntidadValida();
        existente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(pedidoRepository.findByClienteId(id)).thenReturn(List.of(new Pedido()));

        assertThatThrownBy(() -> service.bajaLogica(id))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("pedidos asociados");

        verify(repository, never()).save(any());
    }

    @Test
    void modificar_actualizaSoloLosCamposDeCliente() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Cliente existente = crearEntidadValida();
        existente.setId(id);
        Cliente nuevosDatos = Cliente.builder()
                .nombre("Almacén La Esquina")
                .contacto("3614449999")
                .cuit("20-99999999-9")
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Cliente> resultado = service.modificar(id, nuevosDatos);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Almacén La Esquina");
        assertThat(resultado.get().getId()).isEqualTo(id);
    }
}