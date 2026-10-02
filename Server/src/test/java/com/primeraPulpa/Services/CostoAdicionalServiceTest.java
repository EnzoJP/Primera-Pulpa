package com.primeraPulpa.Services;

import com.primeraPulpa.entities.CostoAdicional;
import com.primeraPulpa.entities.PresentacionCosto;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.CostoAdicionalRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Caso con una dependencia extra que tiene efecto colateral: cada alta/baja/
 * modificación de un CostoAdicional dispara un recálculo de costos en todos
 * los mixes (postAlta/postModificacion/postBaja). Acá se verifica que ese
 * recálculo se dispare, además de las validaciones propias de la entidad.
 */
class CostoAdicionalServiceTest extends AbstractBaseServiceTest<CostoAdicional, Long> {

    private MixService mixService;

    @Override
    protected BaseRepository<CostoAdicional, Long> crearRepositoryMock() {
        return mock(CostoAdicionalRepository.class);
    }

    @Override
    protected BaseService<CostoAdicional, Long> crearService(BaseRepository<CostoAdicional, Long> repository) {
        mixService = mock(MixService.class);
        return new CostoAdicionalService((CostoAdicionalRepository) repository, mixService);
    }

    @Override
    protected CostoAdicional crearEntidadValida() {
        return CostoAdicional.builder()
                .descripcion("Bolsa")
                .valor(150.0)
                .presentacion(PresentacionCosto.TODOS)
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    @Test
    void alta_fallaSiElValorEsNegativo() {
        CostoAdicional invalido = CostoAdicional.builder()
                .descripcion("Bolsa")
                .valor(-10)
                .build();

        assertThatThrownBy(() -> service.alta(invalido))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no puede ser negativo");
    }

    @Test
    void alta_asignaPresentacionTodosPorDefectoSiNoSeIndica() throws ErrorServiceException {
        CostoAdicional sinPresentacion = CostoAdicional.builder()
                .descripcion("Etiqueta")
                .valor(20)
                .build();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CostoAdicional resultado = service.alta(sinPresentacion);

        assertThat(resultado.getPresentacion()).isEqualTo(PresentacionCosto.TODOS);
    }

    @Test
    void alta_disparaRecalculoDeCostosDeTodosLosMixes() throws ErrorServiceException {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.alta(crearEntidadValida());

        verify(mixService, times(1)).recalcularTodosLosCostos();
    }

    @Test
    void bajaLogica_disparaRecalculoDeCostosDeTodosLosMixes() throws ErrorServiceException {
        Long id = idDeEjemplo();
        CostoAdicional existente = crearEntidadValida();
        existente.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.bajaLogica(id);

        verify(mixService, times(1)).recalcularTodosLosCostos();
    }
}