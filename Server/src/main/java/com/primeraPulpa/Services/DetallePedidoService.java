package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class DetallePedidoService extends BaseService<DetallePedido, Long> {

    public DetallePedidoService(DetallePedidoRepository repository) {
        super(repository);
    }

    @Override
    protected void validar(DetallePedido detalle) throws ErrorServiceException {
        if (detalle.getPedido() == null) {
            throw new ErrorServiceException("El detalle debe pertenecer a un pedido");
        }
        if (detalle.getMix() == null) {
            throw new ErrorServiceException("Debe indicar el mix solicitado");
        }
        if (detalle.getCantidad() <= 0) {
            throw new ErrorServiceException("La cantidad debe ser mayor a cero");
        }
        if (detalle.getPrecioUnitario() < 0) {
            throw new ErrorServiceException("El precio unitario no puede ser negativo");
        }
    }
}
