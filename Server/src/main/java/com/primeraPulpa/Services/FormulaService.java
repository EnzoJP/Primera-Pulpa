package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.FormulaRepository;
import org.springframework.stereotype.Service;

@Service
public class FormulaService extends BaseService<Formula, Long> {

    private final MixService mixService;

    public FormulaService(FormulaRepository repository, MixService mixService) {
        super(repository);
        this.mixService = mixService;
    }

    @Override
    protected void validar(Formula formula) throws ErrorServiceException {
        if (formula.getMix() == null) {
            throw new ErrorServiceException("Debe indicar el mix al que pertenece la fórmula");
        }
        if (formula.getMateriaPrima() == null) {
            throw new ErrorServiceException("Debe indicar la materia prima de la fórmula");
        }
        if (formula.getPorcentaje() <= 0 || formula.getPorcentaje() > 100) {
            throw new ErrorServiceException("El porcentaje debe ser mayor a cero y menor o igual a 100");
        }
    }

    @Override
    protected void postAlta(Formula formula) throws ErrorServiceException {
        mixService.recalcularCosto(formula.getMix().getId());
    }

    @Override
    protected void postModificacion(Formula formula) throws ErrorServiceException {
        mixService.recalcularCosto(formula.getMix().getId());
    }

    @Override
    protected void postBaja(Long id) throws ErrorServiceException {
        repository.findById(id).ifPresent(f -> mixService.recalcularCosto(f.getMix().getId()));
    }
}
