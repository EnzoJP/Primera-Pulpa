package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.ClienteRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService extends BaseService<Cliente, Long> {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository repository, PedidoRepository pedidoRepository) {
        super(repository);
        this.clienteRepository = repository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    protected void validar(Cliente cliente) throws ErrorServiceException {
        if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del cliente");
        }
        if (cliente.getContacto() == null || cliente.getContacto().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar un contacto del cliente");
        }
    }

    // HU-12: no se puede eliminar un cliente con pedidos asociados
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        boolean tienePedidos = !pedidoRepository.findByClienteId(id).isEmpty();
        if (tienePedidos) {
            throw new ErrorServiceException("No se puede eliminar un cliente con pedidos asociados");
        }
    }

    public List<Cliente> buscar(String query) {
        if (query == null || query.trim().isEmpty()) {
            return listarActivos();
        }
        return clienteRepository
                .findByNombreContainingIgnoreCaseOrContactoContainingIgnoreCase(query.trim(), query.trim())
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.getEliminado()))
                .toList();
    }

    @Override
    public java.util.Optional<Cliente> modificar(Long id, Cliente entidadNueva) throws ErrorServiceException {
        validar(entidadNueva);
        preModificacion(entidadNueva);
        return clienteRepository.findById(id).map(existente -> {
            existente.setNombre(entidadNueva.getNombre());
            existente.setCuit(entidadNueva.getCuit());
            existente.setContacto(entidadNueva.getContacto());
            Cliente guardado = clienteRepository.save(existente);
            try {
                postModificacion(guardado);
            } catch (ErrorServiceException ignored) {}
            return guardado;
        });
    }
}
