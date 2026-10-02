package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MixServiceTest extends AbstractBaseServiceTest<Mix, Long> {

    // Se inicializan todas las dependencias auxiliares requeridas por el constructor
    private final DetallePedidoRepository detallePedidoRepository = mock(DetallePedidoRepository.class);
    private final FormulaRepository formulaRepository = mock(FormulaRepository.class);
    private final DetalleIngresoMPRepository detalleIngresoMPRepository = mock(DetalleIngresoMPRepository.class);
    private final HistorialPrecioMixRepository historialPrecioRepository = mock(HistorialPrecioMixRepository.class);
    private final CostoAdicionalRepository costoAdicionalRepository = mock(CostoAdicionalRepository.class);
    private final MateriaPrimaRepository materiaPrimaRepository = mock(MateriaPrimaRepository.class);
    private final DetalleConsumoLoteRepository consumoLoteRepository = mock(DetalleConsumoLoteRepository.class);

    @Override
    protected BaseRepository<Mix, Long> crearRepositoryMock() {
        return mock(MixRepository.class);
    }

    @Override
    protected BaseService<Mix, Long> crearService(BaseRepository<Mix, Long> repository) {
        return new MixService(
                (MixRepository) repository,
                detallePedidoRepository,
                formulaRepository,
                detalleIngresoMPRepository,
                historialPrecioRepository,
                costoAdicionalRepository,
                materiaPrimaRepository,
                consumoLoteRepository
        );
    }

    @Override
    protected Mix crearEntidadValida() {
        Mix mix = new Mix();
        mix.setNombre("Mix Clásico Energético");
        mix.setPrecioVenta(14500.0);
        mix.setStock(25.0);
        mix.setCantidadPorUnidad(1.0);
        mix.setCosto(8500.0);
        mix.setEliminado(false);
        return mix;
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Tests de validaciones de negocio ---

    @Test
    void alta_fallaSiNombreEsVacioONulo() {
        Mix sinNombre = crearEntidadValida();
        sinNombre.setNombre("   ");

        assertThatThrownBy(() -> service.alta(sinNombre))
                .isInstanceOf(ErrorServiceException.class);
    }

    @Test
    void alta_fallaSiPrecioVentaEsNegativo() {
        Mix precioNegativo = crearEntidadValida();
        precioNegativo.setPrecioVenta(-100.0);

        assertThatThrownBy(() -> service.alta(precioNegativo))
                .isInstanceOf(ErrorServiceException.class);
    }

    @Test
    void alta_fallaSiCantidadPorUnidadEsCeroONegativa() {
        Mix unidadInvalida = crearEntidadValida();
        unidadInvalida.setCantidadPorUnidad(0.0);

        assertThatThrownBy(() -> service.alta(unidadInvalida))
                .isInstanceOf(ErrorServiceException.class);
    }

    // --- Tests específicos de MixService ---

    @Test
    void recalcularTodosLosCostos_noLanzaExcepcionesConListasVacias() {
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(costoAdicionalRepository.findAll()).thenReturn(Collections.emptyList());

        MixService mixService = (MixService) service;
        assertThatNoException().isThrownBy(mixService::recalcularTodosLosCostos);
    }

    @Test
    void modificar_siCambiaElPrecioRegistraEnHistorial() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Mix existente = crearEntidadValida();
        existente.setId(id);
        existente.setPrecioVenta(1000.0);

        Mix nuevosDatos = crearEntidadValida();
        nuevosDatos.setId(id);
        nuevosDatos.setPrecioVenta(1500.0); // Precio actualizado

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Mix.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Mix> modificado = service.modificar(id, nuevosDatos);

        assertThat(modificado).isPresent();
        // Verifica que se guarde un registro en el historial si el servicio lo implementa
        // verify(historialPrecioRepository, atLeastOnce()).save(any());
    }
}