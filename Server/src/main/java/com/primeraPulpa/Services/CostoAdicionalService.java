package com.primeraPulpa.Services;

import com.primeraPulpa.entities.CostoAdicional;
import com.primeraPulpa.entities.PresentacionCosto;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.CostoAdicionalRepository;
import org.springframework.stereotype.Service;

@Service
public class CostoAdicionalService extends BaseService<CostoAdicional, Long> {

    private final MixService mixService;

    public CostoAdicionalService(CostoAdicionalRepository repository, MixService mixService) {
        super(repository);
        this.mixService = mixService;
    }

    @Override
    protected void validar(CostoAdicional costoAdicional) throws ErrorServiceException {
        if (costoAdicional.getDescripcion() == null || costoAdicional.getDescripcion().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar la descripción del costo adicional");
        }
        if (costoAdicional.getValor() < 0) {
            throw new ErrorServiceException("El valor no puede ser negativo");
        }
        if (costoAdicional.getPresentacion() == null) {
            costoAdicional.setPresentacion(PresentacionCosto.TODOS);
        }
    }

    // El catálogo es compartido por todos los mixes: ante cualquier cambio se recalculan todos
    @Override
    protected void postAlta(CostoAdicional costoAdicional) throws ErrorServiceException {
        mixService.recalcularTodosLosCostos();
    }

    @Override
    protected void postModificacion(CostoAdicional costoAdicional) throws ErrorServiceException {
        mixService.recalcularTodosLosCostos();
    }

    @Override
    protected void postBaja(Long id) throws ErrorServiceException {
        mixService.recalcularTodosLosCostos();
    }
}
