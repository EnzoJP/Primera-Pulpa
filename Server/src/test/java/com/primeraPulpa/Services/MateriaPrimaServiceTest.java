package com.primeraPulpa.Services;

import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.DetalleFormulaRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MateriaPrimaServiceTest extends AbstractBaseServiceTest<MateriaPrima, Long> {

    private final DetalleFormulaRepository detalleFormulaRepository = mock(DetalleFormulaRepository.class);
    private final MixService mixService = mock(MixService.class);

    @Override
    protected BaseRepository<MateriaPrima, Long> crearRepositoryMock() {
        return mock(MateriaPrimaRepository.class);
    }

    @Override
    protected BaseService<MateriaPrima, Long> crearService(BaseRepository<MateriaPrima, Long> repository) {
        // Por defecto en los tests base, asumimos que no está referenciada en fórmulas para permitir la baja
        when(detalleFormulaRepository.findByMateriaPrimaId(any())).thenReturn(Collections.emptyList());

        return new MateriaPrimaService(
                (MateriaPrimaRepository) repository,
                detalleFormulaRepository,
                mixService
        );
    }

    @Override
    protected MateriaPrima crearEntidadValida() {
        UnidadMedida umKg = UnidadMedida.builder().descripcion("kg").build();
        umKg.setId(1L);

        MateriaPrima mp = new MateriaPrima(
                "Almendras Nonpareil",
                umKg,
                12500.0,
                0.0,
                30.0,
                LocalDate.now()
        );
        mp.setEliminado(false);
        return mp;
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Tests de Validaciones ---

    @Test
    void alta_fallaSiNombreEsVacioONulo() {
        MateriaPrima mp = crearEntidadValida();
        mp.setNombre("   ");

        assertThatThrownBy(() -> service.alta(mp))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar el nombre");
    }

    @Test
    void alta_fallaSiUnidadMedidaEsNula() {
        MateriaPrima mp = crearEntidadValida();
        mp.setUnidadMedida(null);

        assertThatThrownBy(() -> service.alta(mp))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar la unidad de medida");
    }

    @Test
    void alta_fallaSiPrecioEsNegativo() {
        MateriaPrima mp = crearEntidadValida();
        mp.setPrecio(-100.0);

        assertThatThrownBy(() -> service.alta(mp))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El precio no puede ser negativo");
    }

    @Test
    void alta_fallaSiStockMinimoEsNegativo() {
        MateriaPrima mp = crearEntidadValida();
        mp.setCantidadMinima(-10.0);

        assertThatThrownBy(() -> service.alta(mp))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El stock mínimo no puede ser negativo");
    }

    // --- Reglas de Negocio HU-03 (preAlta y Stock Inicial) ---

    @Test
    void alta_fuerzaStockInicialEnCeroYAsignaFechaIngresoSiEsNula() throws ErrorServiceException {
        MateriaPrima mp = crearEntidadValida();
        mp.setCantidadActual(500.0); // Intento de enviar stock inicial arbitrario
        mp.setFechaIngreso(null);

        when(repository.save(any(MateriaPrima.class))).thenAnswer(inv -> inv.getArgument(0));

        MateriaPrima guardada = service.alta(mp);

        assertThat(guardada.getCantidadActual()).isEqualTo(0.0);
        assertThat(guardada.getFechaIngreso()).isNotNull();
    }

    // --- HU-03: Restricción de Baja Lógica con Fórmulas ---

    @Test
    void bajaLogica_fallaSiLaMateriaPrimaEstaEnUnaFormula() {
        Long id = idDeEjemplo();
        DetalleFormula detalle = mock(DetalleFormula.class);
        when(detalleFormulaRepository.findByMateriaPrimaId(id)).thenReturn(List.of(detalle));

        assertThatThrownBy(() -> service.bajaLogica(id))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No se puede dar de baja una materia prima utilizada en una fórmula");

        verify(repository, never()).save(any());
    }

    // --- Modificación y Efecto Colateral (Recálculo de Costos) ---

    @Test
    void modificar_conservaStockExistenteYDisparaRecalculoDeMixesAfectados() throws ErrorServiceException {
        Long id = idDeEjemplo();

        MateriaPrima existente = crearEntidadValida();
        existente.setId(id);
        existente.setCantidadActual(80.0); // Stock existente previo
        existente.setFechaIngreso(LocalDate.now().minusMonths(1));
        existente.setEliminado(false);

        MateriaPrima nuevosDatos = crearEntidadValida();
        nuevosDatos.setPrecio(15000.0); // Precio nuevo
        nuevosDatos.setCantidadActual(0.0); // El formulario no manda stock

        // Mock para la fórmula y el mix asociado
        Mix mix = new Mix();
        mix.setId(99L);
        Formula formula = new Formula();
        formula.setMix(mix);

        DetalleFormula df = new DetalleFormula();
        df.setFormula(formula);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(MateriaPrima.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalleFormulaRepository.findByMateriaPrimaId(id)).thenReturn(List.of(df));

        Optional<MateriaPrima> resultado = service.modificar(id, nuevosDatos);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCantidadActual()).isEqualTo(80.0); // Debe conservar el stock
        verify(mixService, times(1)).recalcularCosto(99L); // Debe disparar el recálculo
    }
}