package com.primeraPulpa.Services;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MateriaPrimaService extends BaseService<MateriaPrima, Long> {

    private final FormulaRepository formulaRepository;

    public MateriaPrimaService(MateriaPrimaRepository repository, FormulaRepository formulaRepository) {
        super(repository);
        this.formulaRepository = formulaRepository;
    }

    @Override
    protected void validar(MateriaPrima materiaPrima) throws ErrorServiceException {
        if (materiaPrima.getNombre() == null || materiaPrima.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre de la materia prima");
        }
        if (materiaPrima.getUnidadMedida() == null || materiaPrima.getUnidadMedida().trim().isEmpty()) {
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

    // HU-03: no se puede dar de baja una materia prima referenciada en una fórmula
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        boolean referenciada = !formulaRepository.findByMateriaPrimaId(id).isEmpty();
        if (referenciada) {
            throw new ErrorServiceException("No se puede dar de baja una materia prima utilizada en una fórmula");
        }
    }
}
