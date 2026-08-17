package com.primeraPulpa.Services;

import com.primeraPulpa.entities.CostoAdicional;
import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.CostoAdicionalRepository;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.repositories.MixRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MixService extends BaseService<Mix, Long> {

    private final DetallePedidoRepository detallePedidoRepository;
    private final FormulaRepository formulaRepository;
    private final CostoAdicionalRepository costoAdicionalRepository;

    public MixService(MixRepository repository, DetallePedidoRepository detallePedidoRepository,
                      FormulaRepository formulaRepository, CostoAdicionalRepository costoAdicionalRepository) {
        super(repository);
        this.detallePedidoRepository = detallePedidoRepository;
        this.formulaRepository = formulaRepository;
        this.costoAdicionalRepository = costoAdicionalRepository;
    }

    @Override
    protected void validar(Mix mix) throws ErrorServiceException {
        if (mix.getNombre() == null || mix.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre del mix");
        }
        if (mix.getPrecioVenta() != null && mix.getPrecioVenta() < 0) {
            throw new ErrorServiceException("El precio de venta no puede ser negativo");
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

    // Recalcula el costo por kg del mix:
    //   costo fórmula = (Σ gramos_materiaPrima / 1000 * precio) / cantidad que produce la fórmula
    //   costo mix = costo fórmula + Σ valores de costos adicionales (bolsa, etiqueta...)
    @Transactional
    public void recalcularCosto(Long mixId) {

        Mix mix = repository.findById(mixId).orElse(null);
        if (mix == null) {
            return;
        }


        double costoFormula = 0;
        Formula formula = formulaRepository.findByMixId(mixId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getEliminado()))
                .findFirst()
                .orElse(null);
        if (formula != null && formula.getCantidad() > 0) {
            costoFormula = formula.getCosto();
        }
        /*if (formula != null && formula.getCantidad() > 0) {
            double costoTotal = formula.getDetalles().stream()
                    .filter(d -> d.getMateriaPrima() != null && d.getGramos() > 0)
                    .mapToDouble(d -> (d.getGramos() / 1000.0) * d.getMateriaPrima().getPrecio())
                    .sum();
            costoFormula = costoTotal / formula.getCantidad();
            }*/


        double costoAdicional = costoAdicionalRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getEliminado()))
                .mapToDouble(CostoAdicional::getValor)
                .sum();

        mix.setCosto(costoFormula + costoAdicional);
        repository.save(mix);
    }

    @Transactional
    public void recalcularTodosLosCostos() {
        repository.findAll().stream()
                .filter(m -> !Boolean.TRUE.equals(m.getEliminado()))
                .forEach(m -> recalcularCosto(m.getId()));
    }
}
