package com.primeraPulpa.Services;

import com.primeraPulpa.entities.BaseEntity;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Base de tests unitarios para cualquier Service que extienda BaseService<T, ID>.
 *
 * Contiene los 7 tests comunes a todo ABM (alta, modificar, bajaLogica, obtener,
 * listarActivos, getOne), probados contra el repository MOCKEADO: no levanta
 * Spring ni se conecta a PostgreSQL
 */
@ActiveProfiles("test")
@Transactional
public abstract class AbstractBaseServiceTest<T extends BaseEntity<ID>, ID> {

    protected BaseRepository<T, ID> repository;
    protected BaseService<T, ID> service;

    /** Mock del repository específico de la entidad, ej: mock(RolRepository.class). */
    protected abstract BaseRepository<T, ID> crearRepositoryMock();

    /** Instancia el service real pasándole el repository mockeado (y cualquier otra dependencia, también mockeada). */
    protected abstract BaseService<T, ID> crearService(BaseRepository<T, ID> repository);

    /** Una entidad válida, lista para alta/modificación exitosas. */
    protected abstract T crearEntidadValida();

    /** Un id de ejemplo, usado en los tests de modificar/baja/obtener. */
    protected abstract ID idDeEjemplo();

    @BeforeEach
    void setUpBase() {
        repository = crearRepositoryMock();
        service = crearService(repository);
    }

    @Test
    void alta_guardaYDevuelveLaEntidadConEliminadoFalse() throws ErrorServiceException {
        T entidad = crearEntidadValida();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        T resultado = service.alta(entidad);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEliminado()).isFalse();
        verify(repository, times(1)).save(any());
    }

    @Test
    void modificar_actualizaCuandoExiste() throws ErrorServiceException {
        ID id = idDeEjemplo();
        T existente = crearEntidadValida();
        existente.setId(id);
        T nuevosDatos = crearEntidadValida();

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<T> resultado = service.modificar(id, nuevosDatos);

        assertThat(resultado).isPresent();
        verify(repository).save(any());
    }

    @Test
    void modificar_devuelveVacioCuandoNoExiste() throws ErrorServiceException {
        ID id = idDeEjemplo();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<T> resultado = service.modificar(id, crearEntidadValida());

        assertThat(resultado).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void bajaLogica_marcaEliminadoTrueCuandoExiste() throws ErrorServiceException {
        ID id = idDeEjemplo();
        T existente = crearEntidadValida();
        existente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean resultado = service.bajaLogica(id);

        assertThat(resultado).isTrue();
        assertThat(existente.getEliminado()).isTrue();
    }

    @Test
    void bajaLogica_devuelveFalseCuandoNoExiste() throws ErrorServiceException {
        ID id = idDeEjemplo();
        when(repository.findById(id)).thenReturn(Optional.empty());

        boolean resultado = service.bajaLogica(id);

        assertThat(resultado).isFalse();
    }

    @Test
    void obtener_devuelveVacioSiLaEntidadEstaEliminada() throws ErrorServiceException {
        ID id = idDeEjemplo();
        T entidad = crearEntidadValida();
        entidad.setId(id);
        entidad.setEliminado(true);

        when(repository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<T> resultado = service.obtener(id);

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarActivos_excluyeLosEliminados() throws ErrorServiceException {
        T activo = crearEntidadValida();
        activo.setEliminado(false);
        T eliminado = crearEntidadValida();
        eliminado.setEliminado(true);

        when(repository.findAll()).thenReturn(List.of(activo, eliminado));

        List<T> resultado = service.listarActivos();

        assertThat(resultado).containsExactly(activo);
    }

    @Test
    void getOne_lanzaExcepcionSiNoExiste() {
        ID id = idDeEjemplo();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOne(id))
                .isInstanceOf(ErrorServiceException.class);
    }
}