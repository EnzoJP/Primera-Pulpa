package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.RolRepository;
import org.springframework.stereotype.Service;

@Service
public class RolService extends BaseService<Rol, Long> {

    public RolService(RolRepository repository) {
        super(repository);
    }

    @Override
    protected void validar(Rol rol) throws ErrorServiceException {
        if (rol.getDescripcion() == null || rol.getDescripcion().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar la descripción del rol");
        }
    }
}
