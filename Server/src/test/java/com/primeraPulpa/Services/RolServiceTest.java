package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.RolRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Caso más simple: Rol no tiene dependencias extra, solo su repository.
 * Ya hereda los 7 tests de ABM de AbstractBaseServiceTest; acá solo se agrega
 * el test de la validación específica de esta entidad (validar()).
 */
class RolServiceTest extends AbstractBaseServiceTest<Rol, Long> {

    @Override
    protected BaseRepository<Rol, Long> crearRepositoryMock() {
        return mock(RolRepository.class);
    }

    @Override
    protected BaseService<Rol, Long> crearService(BaseRepository<Rol, Long> repository) {
        return new RolService((RolRepository) repository);
    }

    @Override
    protected Rol crearEntidadValida() {
        return Rol.builder()
                .descripcion("Administrador")
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    @Test
    void alta_fallaSiNoTieneDescripcion() {
        Rol rolInvalido = Rol.builder().descripcion("   ").build();

        assertThatThrownBy(() -> service.alta(rolInvalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("descripción del rol");
    }
}