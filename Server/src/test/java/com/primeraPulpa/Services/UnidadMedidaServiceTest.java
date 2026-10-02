package com.primeraPulpa.Services;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.UnidadMedida;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.UnidadMedidaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Caso con una dependencia extra: UnidadMedidaService también necesita
 * MateriaPrimaRepository para chequear referencias antes de dar de baja.
 * Esa dependencia se mockea acá y se guarda en un campo para poder
 * configurarla distinto en el test específico de abajo.
 */
class UnidadMedidaServiceTest extends AbstractBaseServiceTest<UnidadMedida, Long> {

    private MateriaPrimaRepository materiaPrimaRepository;

    @Override
    protected BaseRepository<UnidadMedida, Long> crearRepositoryMock() {
        return mock(UnidadMedidaRepository.class);
    }

    @Override
    protected BaseService<UnidadMedida, Long> crearService(BaseRepository<UnidadMedida, Long> repository) {
        materiaPrimaRepository = mock(MateriaPrimaRepository.class);
        // Por defecto, ninguna materia prima usa esta unidad (para que los 7 tests heredados no fallen).
        when(materiaPrimaRepository.findByUnidadMedidaId(any())).thenReturn(List.of());
        return new UnidadMedidaService((UnidadMedidaRepository) repository, materiaPrimaRepository);
    }

    @Override
    protected UnidadMedida crearEntidadValida() {
        return UnidadMedida.builder()
                .descripcion("Kilogramo")
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    @Test
    void bajaLogica_fallaSiLaUnidadEstaUsadaPorUnaMateriaPrima() {
        Long id = idDeEjemplo();
        UnidadMedida existente = crearEntidadValida();
        existente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(materiaPrimaRepository.findByUnidadMedidaId(id))
                .thenReturn(List.of(new MateriaPrima()));

        assertThatThrownBy(() -> service.bajaLogica(id))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No se puede dar de baja");

        verify(repository, never()).save(any());
    }
}