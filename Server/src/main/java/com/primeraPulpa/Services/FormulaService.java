package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetalleFormula;
import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.FormulaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
            throw new ErrorServiceException("Debe indicar el mix de la fórmula");
        }
        if (formula.getCantidad() <= 0) {
            throw new ErrorServiceException("Debe indicar la cantidad que produce la fórmula");
        }
        boolean hayDetalles = formula.getDetalles() != null &&
                formula.getDetalles().stream()
                        .anyMatch(d -> d.getMateriaPrima() != null && d.getGramos() > 0);
        if (!hayDetalles) {
            throw new ErrorServiceException("Debe cargar al menos una materia prima con gramos");
        }
    }

    @Override
    protected void preAlta(Formula formula) throws ErrorServiceException {
        prepararDetalles(formula);
    }

    @Override
    protected void preModificacion(Formula formula) throws ErrorServiceException {
        prepararDetalles(formula);
    }

    // Deja solo los detalles con gramos > 0 y los vincula a la fórmula
    private void prepararDetalles(Formula formula) {
        List<DetalleFormula> usados = new ArrayList<>();
        for (DetalleFormula d : formula.getDetalles()) {
            if (d.getMateriaPrima() != null && d.getGramos() > 0) {
                d.setFormula(formula);
                usados.add(d);
            }
        }
        formula.setDetalles(usados);
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
