package com.primeraPulpa.Services;

import com.primeraPulpa.entities.EstadoPedido;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.EstadoPedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class EstadoPedidoService extends BaseService<EstadoPedido, Long> {

    public EstadoPedidoService(EstadoPedidoRepository repository) {
        super(repository);
    }

    @Override
    protected void validar(EstadoPedido estadoPedido) throws ErrorServiceException {
        if (estadoPedido.getDescripcion() == null || estadoPedido.getDescripcion().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar la descripción del estado");
        }
    }
}
