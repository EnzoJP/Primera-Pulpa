package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.repositories.MixRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MixService extends BaseService<Mix, Long> {

    private final DetallePedidoRepository detallePedidoRepository;
    private final FormulaRepository formulaRepository;

    public MixService(MixRepository repository, DetallePedidoRepository detallePedidoRepository,
                      FormulaRepository formulaRepository) {
        super(repository);
        this.detallePedidoRepository = detallePedidoRepository;
        this.formulaRepository = formulaRepository;
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

    // Recalcula el costo del mix a partir de sus fórmulas:
    // costo = suma de (porcentaje / 100) * precio de cada materia prima.
    @Transactional
    public void recalcularCosto(Long mixId) {
        Mix mix = repository.findById(mixId).orElse(null);
        if (mix == null) {
            return;
        }
        double costo = formulaRepository.findByMixId(mixId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .mapToDouble(f -> (f.getPorcentaje() / 100.0) * f.getMateriaPrima().getPrecio())
                .sum();
        mix.setCosto(costo);
        repository.save(mix);
    }
}
