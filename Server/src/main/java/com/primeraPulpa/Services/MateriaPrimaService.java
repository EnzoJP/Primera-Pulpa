package com.primeraPulpa.Services;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetalleFormulaRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MateriaPrimaService extends BaseService<MateriaPrima, Long> {

    private final DetalleFormulaRepository detalleFormulaRepository;
    private final MixService mixService;

    public MateriaPrimaService(MateriaPrimaRepository repository, DetalleFormulaRepository detalleFormulaRepository,
                               MixService mixService) {
        super(repository);
        this.detalleFormulaRepository = detalleFormulaRepository;
        this.mixService = mixService;
    }

    @Override
    protected void validar(MateriaPrima materiaPrima) throws ErrorServiceException {
        if (materiaPrima.getNombre() == null || materiaPrima.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre de la materia prima");
        }
        if (materiaPrima.getUnidadMedida() == null) {
            throw new ErrorServiceException("Debe indicar la unidad de medida");
        }
        if (materiaPrima.getPrecio() < 0) {
            throw new ErrorServiceException("El precio no puede ser negativo");
        }
        if (materiaPrima.getCantidadMinima() < 0) {
            throw new ErrorServiceException("El stock mínimo no puede ser negativo");
        }
    }

    @Override
    protected void preAlta(MateriaPrima materiaPrima) throws ErrorServiceException {
        // HU-03: el stock siempre arranca en 0, sin importar lo que venga en el request
        materiaPrima.setCantidadActual(0);
        if (materiaPrima.getFechaIngreso() == null) {
            materiaPrima.setFechaIngreso(LocalDate.now());
        }
    }

    // Si cambia el precio, se recalculan los costos de los mixes que la usan
    @Override
    protected void postModificacion(MateriaPrima materiaPrima) throws ErrorServiceException {
        detalleFormulaRepository.findByMateriaPrimaId(materiaPrima.getId()).stream()
                .map(d -> d.getFormula().getMix().getId())
                .distinct()
                .forEach(mixService::recalcularCosto);
    }

    // HU-03: no se puede dar de baja una materia prima usada en una fórmula
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        boolean referenciada = !detalleFormulaRepository.findByMateriaPrimaId(id).isEmpty();
        if (referenciada) {
            throw new ErrorServiceException("No se puede dar de baja una materia prima utilizada en una fórmula");
        }
    }
}
