package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.FormulaRepository;
import org.springframework.stereotype.Service;

@Service
public class FormulaService extends BaseService<Formula, Long> {

    public FormulaService(FormulaRepository repository) {
        super(repository);
    }

    @Override
    protected void validar(Formula formula) throws ErrorServiceException {
        if (formula.getMix() == null) {
            throw new ErrorServiceException("Debe indicar el mix al que pertenece la fórmula");
        }
        if (formula.getMateriaPrima() == null) {
            throw new ErrorServiceException("Debe indicar la materia prima de la fórmula");
        }
        if (formula.getCantidad() <= 0) {
            throw new ErrorServiceException("La cantidad debe ser mayor a cero");
        }
    }
}
