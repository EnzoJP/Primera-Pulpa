package com.primeraPulpa.Services;

import com.primeraPulpa.entities.UnidadMedida;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.UnidadMedidaRepository;
import org.springframework.stereotype.Service;

@Service
public class UnidadMedidaService extends BaseService<UnidadMedida, Long> {

    private final MateriaPrimaRepository materiaPrimaRepository;

    public UnidadMedidaService(UnidadMedidaRepository repository, MateriaPrimaRepository materiaPrimaRepository) {
        super(repository);
        this.materiaPrimaRepository = materiaPrimaRepository;
    }

    @Override
    protected void validar(UnidadMedida unidadMedida) throws ErrorServiceException {
        if (unidadMedida.getDescripcion() == null || unidadMedida.getDescripcion().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar la descripción de la unidad de medida");
        }
    }

    // No se puede dar de baja una unidad de medida utilizada por una materia prima
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        boolean referenciada = !materiaPrimaRepository.findByUnidadMedidaId(id).isEmpty();
        if (referenciada) {
            throw new ErrorServiceException("No se puede dar de baja una unidad de medida utilizada por una materia prima");
        }
    }
}
