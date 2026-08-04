package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.ClienteRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class ClienteService extends BaseService<Cliente, Long> {

    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository repository, PedidoRepository pedidoRepository) {
        super(repository);
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
}
