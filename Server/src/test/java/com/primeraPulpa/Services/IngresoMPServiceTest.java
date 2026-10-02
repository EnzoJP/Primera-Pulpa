package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.IngresoMP;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.BaseRepository;
import com.primeraPulpa.repositories.DetalleIngresoMPRepository;
import com.primeraPulpa.repositories.IngresoMPRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngresoMPServiceTest extends AbstractBaseServiceTest<IngresoMP, Long> {

    private final DetalleIngresoMPRepository detalleRepository = mock(DetalleIngresoMPRepository.class);
    private final MateriaPrimaRepository materiaPrimaRepository = mock(MateriaPrimaRepository.class);

    @Override
    protected BaseRepository<IngresoMP, Long> crearRepositoryMock() {
        return mock(IngresoMPRepository.class);
    }

    @Override
    protected BaseService<IngresoMP, Long> crearService(BaseRepository<IngresoMP, Long> repository) {
        return new IngresoMPService(
                (IngresoMPRepository) repository,
                detalleRepository,
                materiaPrimaRepository
        );
    }

    @Override
    protected IngresoMP crearEntidadValida() {
        return IngresoMP.builder()
                .fechaHora(LocalDateTime.now())
                .usuario(Usuario.builder().nombre("Operario").build())
                .build();
    }

    @Override
    protected Long idDeEjemplo() {
        return 1L;
    }

    // --- Tests de Validaciones de registrar() ---

    @Test
    void registrar_fallaSiListaDeDetallesEsNulaOVacia() {
        IngresoMPService ingresoService = (IngresoMPService) service;

        assertThatThrownBy(() -> ingresoService.registrar(null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El ingreso debe tener al menos una materia prima.");

        assertThatThrownBy(() -> ingresoService.registrar(Collections.emptyList()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El ingreso debe tener al menos una materia prima.");
    }

    @Test
    void registrar_fallaSiDetalleNoTieneMateriaPrimaOId() {
        IngresoMPService ingresoService = (IngresoMPService) service;

        DetalleIngresoMP detSinMp = DetalleIngresoMP.builder()
                .cantidad(10.0)
                .materiaPrima(null)
                .build();

        assertThatThrownBy(() -> ingresoService.registrar(List.of(detSinMp)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Cada detalle debe indicar la materia prima.");

        MateriaPrima mpSinId = new MateriaPrima();
        DetalleIngresoMP detSinId = DetalleIngresoMP.builder()
                .cantidad(10.0)
                .materiaPrima(mpSinId)
                .build();

        assertThatThrownBy(() -> ingresoService.registrar(List.of(detSinId)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Cada detalle debe indicar la materia prima.");
    }

    @Test
    void registrar_fallaSiCantidadEsCeroONegativa() {
        IngresoMPService ingresoService = (IngresoMPService) service;

        MateriaPrima mp = new MateriaPrima();
        mp.setId(5L);

        DetalleIngresoMP detCero = DetalleIngresoMP.builder()
                .materiaPrima(mp)
                .cantidad(0.0)
                .build();

        assertThatThrownBy(() -> ingresoService.registrar(List.of(detCero)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La cantidad debe ser mayor a cero.");

        DetalleIngresoMP detNegativo = DetalleIngresoMP.builder()
                .materiaPrima(mp)
                .cantidad(-15.0)
                .build();

        assertThatThrownBy(() -> ingresoService.registrar(List.of(detNegativo)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("La cantidad debe ser mayor a cero.");
    }

    @Test
    void registrar_fallaSiMateriaPrimaNoExisteEnBaseDeDatos() {
        IngresoMPService ingresoService = (IngresoMPService) service;

        MateriaPrima mp = new MateriaPrima();
        mp.setId(99L);

        DetalleIngresoMP detalle = DetalleIngresoMP.builder()
                .materiaPrima(mp)
                .cantidad(50.0)
                .build();

        when(materiaPrimaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoService.registrar(List.of(detalle)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Materia prima no encontrada.");
    }

    // --- Tests del flujo exitoso de registrar() (HU-04) ---

    @Test
    void registrar_guardaCabeceraDetallesYActualizaStockDeMateriaPrima() throws ErrorServiceException {
        IngresoMPService ingresoService = (IngresoMPService) service;

        MateriaPrima mp = new MateriaPrima();
        mp.setId(10L);
        mp.setNombre("Almendras");
        mp.setPrecio(12000.0);
        mp.setCantidadActual(30.0);
        mp.setFechaIngreso(LocalDate.now().minusDays(20));

        DetalleIngresoMP detalle = DetalleIngresoMP.builder()
                .materiaPrima(mp)
                .cantidad(70.0)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .numeroLote("LOT-ALM-2026")
                .build();

        Usuario usuario = Usuario.builder().nombre("Recepcionista").build();

        when(materiaPrimaRepository.findById(10L)).thenReturn(Optional.of(mp));
        when(repository.save(any(IngresoMP.class))).thenAnswer(inv -> {
            IngresoMP i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        IngresoMP resultado = ingresoService.registrar(List.of(detalle), usuario);

        // 1. Cabecera guardada
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        assertThat(resultado.getEliminado()).isFalse();

        // 2. Detalle guardado con asignaciones correctas
        verify(detalleRepository, times(1)).save(detalle);
        assertThat(detalle.getIngresoMP()).isEqualTo(resultado);
        assertThat(detalle.getCantidadRestante()).isEqualTo(70.0);
        assertThat(detalle.getCostoUnitario()).isEqualTo(12000.0);
        assertThat(detalle.getEliminado()).isFalse();

        // 3. Stock de Materia Prima actualizado: 30 + 70 = 100
        verify(materiaPrimaRepository, atLeastOnce()).save(mp);
        assertThat(mp.getCantidadActual()).isEqualTo(100.0);
        assertThat(mp.getFechaIngreso()).isEqualTo(resultado.getFechaHora().toLocalDate());
    }
}