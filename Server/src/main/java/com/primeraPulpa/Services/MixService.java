package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.repositories.MixRepository;
import org.springframework.stereotype.Service;

@Service
public class MixService extends BaseService<Mix, Long> {

    private final DetallePedidoRepository detallePedidoRepository;

    public MixService(MixRepository repository, DetallePedidoRepository detallePedidoRepository) {
        super(repository);
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Override
    protected void validar(Mix mix) throws ErrorServiceException {
        if (mix.getNombre() == null || mix.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del mix");
        }
        if (mix.getPrecioVenta() < 0) {
            throw new ErrorServiceException("El precio de venta no puede ser negativo");
        }
    }

    @Override
    protected void preAlta(Mix mix) throws ErrorServiceException {
        if (mix.getCantidadProducida() < 0) {
            mix.setCantidadProducida(0);
        }
    }

    // HU-08: no se puede dar de baja un mix con pedidos asociados
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        boolean tienePedidos = !detallePedidoRepository.findByMixId(id).isEmpty();
        if (tienePedidos) {
            throw new ErrorServiceException("No se puede dar de baja un mix con pedidos asociados");
        }
    }
}
