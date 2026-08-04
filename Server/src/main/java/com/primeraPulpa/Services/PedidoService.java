package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PedidoService extends BaseService<Pedido, Long> {

    public PedidoService(PedidoRepository repository) {
        super(repository);
    }

    @Override
    protected void validar(Pedido pedido) throws ErrorServiceException {
        if (pedido.getCliente() == null) {
            throw new ErrorServiceException("Debe indicar el cliente del pedido");
        }
        if (pedido.getUsuario() == null) {
            throw new ErrorServiceException("Debe indicar el usuario que registra el pedido");
        }
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new ErrorServiceException("El pedido debe tener al menos un mix solicitado");
        }
    }

    @Override
    protected void preAlta(Pedido pedido) throws ErrorServiceException {
        if (pedido.getFecha() == null) {
            pedido.setFecha(LocalDate.now());
        }
        // El estado inicial (PENDIENTE) y el descuento de stock del Mix se resuelven
        // en un método de servicio dedicado (ej. confirmarPedido), no en el alta genérica,
        // porque dependen de cómo terminemos modelando el stock de Mix
    }
}
