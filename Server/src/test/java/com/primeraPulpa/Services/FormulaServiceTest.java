package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetalleFormula;
import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.FormulaRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FormulaServiceTest extends AbstractBaseServiceTest<Formula, Long> {

    private final MixService mixService = mock(MixService.class);

    @Override
    protected BaseRepository<Formula, Long> crearRepositoryMock() {
        return mock(FormulaRepository.class);
    }

    @Override
    protected BaseService<Formula, Long> crearService(BaseRepository<Formula, Long> repository) {
        return new FormulaService((FormulaRepository) repository, mixService);
    }

    @Override
    protected Formula crearEntidadValida() {
        Mix mix = new Mix();
        mix.setId(10L);
        mix.setNombre("Mix Clásico");

        MateriaPrima mp = new MateriaPrima();
        mp.setId(100L);
        mp.setNombre("Almendras");

        Formula formula = new Formula();
        formula.setMix(mix);
        formula.setCantidad(10.0);

        List<DetalleFormula> detalles = new ArrayList<>();
        DetalleFormula detalle = new DetalleFormula();
        detalle.setMateriaPrima(mp);
        detalle.setGramos(2000.0);
        detalles.add(detalle);

        formula.setDetalles(detalles);
        return formula;
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Tests de Validaciones del método validar() ---

    @Test
    void alta_fallaSiMixEsNulo() {
        Formula formula = crearEntidadValida();
        formula.setMix(null);

        assertThatThrownBy(() -> service.alta(formula))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar el mix de la fórmula");
    }

    @Test
    void alta_fallaSiCantidadEsCeroONegativa() {
        Formula formulaCero = crearEntidadValida();
        formulaCero.setCantidad(0.0);

        assertThatThrownBy(() -> service.alta(formulaCero))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar la cantidad que produce la fórmula");

        Formula formulaNegativa = crearEntidadValida();
        formulaNegativa.setCantidad(-5.0);

        assertThatThrownBy(() -> service.alta(formulaNegativa))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar la cantidad que produce la fórmula");
    }

    @Test
    void alta_fallaSiNoTieneDetallesOListaEsVacia() {
        Formula sinDetalles = crearEntidadValida();
        sinDetalles.setDetalles(null);

        assertThatThrownBy(() -> service.alta(sinDetalles))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe cargar al menos una materia prima con gramos");

        Formula detallesVacios = crearEntidadValida();
        detallesVacios.setDetalles(new ArrayList<>());

        assertThatThrownBy(() -> service.alta(detallesVacios))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe cargar al menos una materia prima con gramos");
    }

    @Test
    void alta_fallaSiTodosLosDetallesTienenGramosCeroOMateriaPrimaNula() {
        Formula formula = crearEntidadValida();
        DetalleFormula dInvalido1 = new DetalleFormula();
        dInvalido1.setMateriaPrima(null);
        dInvalido1.setGramos(100.0);

        DetalleFormula dInvalido2 = new DetalleFormula();
        dInvalido2.setMateriaPrima(new MateriaPrima());
        dInvalido2.setGramos(0.0);

        formula.setDetalles(List.of(dInvalido1, dInvalido2));

        assertThatThrownBy(() -> service.alta(formula))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe cargar al menos una materia prima con gramos");
    }

    // --- Tests de preAlta / prepararDetalles ---

    @Test
    void alta_filtraDetallesConGramosCeroYVinculaDetallesConLaFormula() throws ErrorServiceException {
        Formula formula = crearEntidadValida();

        MateriaPrima mpValida = new MateriaPrima();
        mpValida.setId(200L);

        DetalleFormula dValido = new DetalleFormula();
        dValido.setMateriaPrima(mpValida);
        dValido.setGramos(500.0);

        DetalleFormula dSinGramos = new DetalleFormula();
        dSinGramos.setMateriaPrima(mpValida);
        dSinGramos.setGramos(0.0); // No debe incluirse

        formula.setDetalles(new ArrayList<>(List.of(dValido, dSinGramos)));
        when(repository.save(any(Formula.class))).thenAnswer(inv -> inv.getArgument(0));

        Formula creada = service.alta(formula);

        assertThat(creada.getDetalles()).hasSize(1);
        assertThat(creada.getDetalles().get(0).getGramos()).isEqualTo(500.0);
        assertThat(creada.getDetalles().get(0).getFormula()).isEqualTo(creada);
    }

    // --- Tests de Efectos Colaterales (mixService.recalcularCosto) ---

    @Test
    void alta_disparaRecalculoDeCostoDelMix() throws ErrorServiceException {
        Formula formula = crearEntidadValida();
        when(repository.save(any(Formula.class))).thenAnswer(inv -> inv.getArgument(0));

        service.alta(formula);

        verify(mixService, times(1)).recalcularCosto(10L);
    }

    @Test
    void modificar_disparaRecalculoDeCostoDelMix() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Formula existente = crearEntidadValida();
        existente.setId(id);

        Formula nuevosDatos = crearEntidadValida();
        nuevosDatos.setCantidad(20.0);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Formula.class))).thenAnswer(inv -> inv.getArgument(0));

        service.modificar(id, nuevosDatos);

        verify(mixService, times(1)).recalcularCosto(10L);
    }

    @Test
    void bajaLogica_disparaRecalculoDeCostoDelMix() throws ErrorServiceException {
        Long id = idDeEjemplo();
        Formula existente = crearEntidadValida();
        existente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Formula.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean resultado = service.bajaLogica(id);

        assertThat(resultado).isTrue();
        verify(mixService, times(1)).recalcularCosto(10L);
    }
}