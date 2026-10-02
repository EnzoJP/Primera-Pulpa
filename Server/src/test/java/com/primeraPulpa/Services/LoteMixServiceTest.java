package com.primeraPulpa.Services;

import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoteMixServiceTest extends AbstractBaseServiceTest<LoteMix, Long> {

    private final MixRepository mixRepository = mock(MixRepository.class);
    private final MixService mixService = mock(MixService.class);
    private final DetalleConsumoLoteRepository consumoLoteRepository = mock(DetalleConsumoLoteRepository.class);
    private final MateriaPrimaRepository materiaPrimaRepository = mock(MateriaPrimaRepository.class);
    private final DetalleIngresoMPRepository detalleIngresoMPRepository = mock(DetalleIngresoMPRepository.class);

    @Override
    protected BaseRepository<LoteMix, Long> crearRepositoryMock() {
        return mock(LoteMixRepository.class);
    }

    @Override
    protected BaseService<LoteMix, Long> crearService(BaseRepository<LoteMix, Long> repository) {
        return new LoteMixService(
                (LoteMixRepository) repository,
                mixRepository,
                mixService,
                consumoLoteRepository,
                materiaPrimaRepository,
                detalleIngresoMPRepository
        );
    }

    @Override
    protected LoteMix crearEntidadValida() {
        Mix mix = new Mix();
        mix.setId(10L);
        mix.setNombre("Mix Fitness");

        return LoteMix.builder()
                .mix(mix)
                .fechaElaboracion(LocalDate.now())
                .cantidadElaborada(25.0)
                .usuario(Usuario.builder().nombre("Operario").build())
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Tests de Validaciones del método validar() ---

    @Test
    void alta_fallaSiNoTieneMix() {
        LoteMix sinMix = crearEntidadValida();
        sinMix.setMix(null);

        assertThatThrownBy(() -> service.alta(sinMix))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar el mix elaborado.");
    }

    @Test
    void alta_fallaSiFechaElaboracionEsNula() {
        LoteMix sinFecha = crearEntidadValida();
        sinFecha.setFechaElaboracion(null);

        assertThatThrownBy(() -> service.alta(sinFecha))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar la fecha de elaboración.");
    }

    @Test
    void alta_fallaSiCantidadElaboradaEsCeroONegativa() {
        LoteMix conCero = crearEntidadValida();
        conCero.setCantidadElaborada(0.0);

        assertThatThrownBy(() -> service.alta(conCero))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La cantidad elaborada debe ser mayor a cero.");

        LoteMix conNegativo = crearEntidadValida();
        conNegativo.setCantidadElaborada(-5.0);

        assertThatThrownBy(() -> service.alta(conNegativo))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La cantidad elaborada debe ser mayor a cero.");
    }

    // --- Tests específicos de registrarElaboracion ---

    @Test
    void registrarElaboracion_creaNuevoLoteSiNoExisteParaLaFecha() throws ErrorServiceException {
        LoteMixService loteMixService = (LoteMixService) service;
        LoteMixRepository loteRepo = (LoteMixRepository) repository;

        Mix mix = new Mix();
        mix.setId(5L);
        LocalDate hoy = LocalDate.now();
        Usuario usuario = Usuario.builder().nombre("Operario").build();

        when(loteRepo.findByMixAndFechaElaboracionAndEliminadoFalse(mix, hoy))
                .thenReturn(Optional.empty());
        when(loteRepo.save(any(LoteMix.class))).thenAnswer(inv -> inv.getArgument(0));

        LoteMix resultado = loteMixService.registrarElaboracion(mix, hoy, 30.0, usuario);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCantidadElaborada()).isEqualTo(30.0);
        assertThat(resultado.getMix()).isEqualTo(mix);
        verify(mixService, times(1)).actualizarStockMixElaboracion(eq(mix), eq(30.0), any(LoteMix.class), isNull());
    }

    @Test
    void registrarElaboracion_acumulaCantidadSiYaExisteLoteParaLaFecha() throws ErrorServiceException {
        LoteMixService loteMixService = (LoteMixService) service;
        LoteMixRepository loteRepo = (LoteMixRepository) repository;

        Mix mix = new Mix();
        mix.setId(5L);
        LocalDate hoy = LocalDate.now();
        Usuario usuario = Usuario.builder().nombre("Operario").build();

        LoteMix loteExistente = new LoteMix();
        loteExistente.setId(100L);
        loteExistente.setMix(mix);
        loteExistente.setFechaElaboracion(hoy);
        loteExistente.setCantidadElaborada(20.0);

        when(loteRepo.findByMixAndFechaElaboracionAndEliminadoFalse(mix, hoy))
                .thenReturn(Optional.of(loteExistente));
        when(loteRepo.save(any(LoteMix.class))).thenAnswer(inv -> inv.getArgument(0));

        LoteMix resultado = loteMixService.registrarElaboracion(mix, hoy, 15.0, usuario);

        assertThat(resultado.getCantidadElaborada()).isEqualTo(35.0); // 20 + 15
        verify(mixService, times(1)).actualizarStockMixElaboracion(eq(mix), eq(15.0), eq(loteExistente), isNull());
    }

    // --- Test de postBaja: Descarte de DetalleConsumoLote ---

    @Test
    void bajaLogica_marcaEliminadosLosConsumosAsociados() throws ErrorServiceException {
        Long id = idDeEjemplo();
        LoteMix existente = crearEntidadValida();
        existente.setId(id);

        DetalleConsumoLote consumo1 = new DetalleConsumoLote();
        consumo1.setId(101L);
        consumo1.setEliminado(false);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(LoteMix.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumoLoteRepository.findByLoteMixId(id)).thenReturn(List.of(consumo1));

        boolean bajaOk = service.bajaLogica(id);

        assertThat(bajaOk).isTrue();
        assertThat(consumo1.getEliminado()).isTrue();
        verify(consumoLoteRepository, times(1)).save(consumo1);
    }
}