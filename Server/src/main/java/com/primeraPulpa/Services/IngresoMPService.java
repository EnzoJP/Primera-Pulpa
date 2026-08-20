package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.IngresoMP;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetalleIngresoMPRepository;
import com.primeraPulpa.repositories.IngresoMPRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IngresoMPService extends BaseService<IngresoMP, Long> {

    private final DetalleIngresoMPRepository detalleRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;

    public IngresoMPService(IngresoMPRepository ingresoMPRepository,
                            DetalleIngresoMPRepository detalleRepository,
                            MateriaPrimaRepository materiaPrimaRepository) {
        super(ingresoMPRepository);
        this.detalleRepository = detalleRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
    }

    /**
     * Registra un ingreso con sus detalles y actualiza el stock de cada materia prima.
     * Una vez confirmado no puede modificarse (HU-04).
     */
    @Transactional
    public IngresoMP registrar(List<DetalleIngresoMP> detalles) throws ErrorServiceException {
        if (detalles == null || detalles.isEmpty()) {
            throw new ErrorServiceException("El ingreso debe tener al menos una materia prima.");
        }

        // 1. Validar todos los detalles antes de persistir
        for (DetalleIngresoMP detalle : detalles) {
            if (detalle.getMateriaPrima() == null || detalle.getMateriaPrima().getId() == null) {
                throw new ErrorServiceException("Cada detalle debe indicar la materia prima.");
            }
            if (detalle.getCantidad() <= 0) {
                throw new ErrorServiceException("La cantidad debe ser mayor a cero.");
            }

            MateriaPrima mp = materiaPrimaRepository.findById(detalle.getMateriaPrima().getId())
                    .orElseThrow(() -> new ErrorServiceException("Materia prima no encontrada."));

        }

        // 2. Guardar cabecera de ingreso
        IngresoMP ingreso = IngresoMP.builder()
                .fechaHora(LocalDateTime.now())
                .build();
        ingreso.setEliminado(false);
        IngresoMP ingresoGuardado = repository.save(ingreso);

        // 3. Guardar detalles y actualizar stock
        // La fecha del ingreso pasa a ser la última fecha de ingreso de cada materia prima.
        // El histórico de cada recepción (con su propio vencimiento) queda en IngresoMP/DetalleIngresoMP.
        LocalDate fechaIngreso = ingresoGuardado.getFechaHora().toLocalDate();
        for (DetalleIngresoMP detalle : detalles) {
            MateriaPrima mp = materiaPrimaRepository.findById(detalle.getMateriaPrima().getId()).get();

            detalle.setIngresoMP(ingresoGuardado);
            detalle.setEliminado(false);
            detalle.setCantidadRestante(detalle.getCantidad());
            detalleRepository.save(detalle);

            // Actualizar stock de la materia prima
            mp.setCantidadActual(mp.getCantidadActual() + detalle.getCantidad());
            mp.setFechaIngreso(fechaIngreso);
            materiaPrimaRepository.save(mp);
        }

        return ingresoGuardado;
    }

    @Override
    protected void validar(IngresoMP ingreso) throws ErrorServiceException {
        // La validación principal se hace en registrar()
    }
}
