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
            if (detalle.getCostoUnitario() < 0) {
                throw new ErrorServiceException("El costo unitario no puede ser negativo.");
            }

            MateriaPrima mp = materiaPrimaRepository.findById(detalle.getMateriaPrima().getId())
                    .orElseThrow(() -> new ErrorServiceException("Materia prima no encontrada."));

            if (detalle.getCantidad() < mp.getCantidadMinima()) {
                String unidad = mp.getUnidadMedida() != null ? (" " + mp.getUnidadMedida().getDescripcion()) : "";
                throw new ErrorServiceException("La cantidad ingresada para '" + mp.getNombre() + "' (" + detalle.getCantidad() + unidad +
                        ") no puede ser menor al stock mínimo establecido (" + mp.getCantidadMinima() + unidad + ").");
            }
        }

        // 2. Guardar cabecera de ingreso
        IngresoMP ingreso = IngresoMP.builder()
                .fechaHora(LocalDateTime.now())
                .build();
        ingreso.setEliminado(false);
        IngresoMP ingresoGuardado = repository.save(ingreso);

        // 3. Guardar detalles y actualizar stock
        for (DetalleIngresoMP detalle : detalles) {
            MateriaPrima mp = materiaPrimaRepository.findById(detalle.getMateriaPrima().getId()).get();

            detalle.setIngresoMP(ingresoGuardado);
            detalle.setEliminado(false);
            detalleRepository.save(detalle);

            // Actualizar stock de la materia prima
            mp.setCantidadActual(mp.getCantidadActual() + detalle.getCantidad());
            materiaPrimaRepository.save(mp);
        }

        return ingresoGuardado;
    }

    @Override
    protected void validar(IngresoMP ingreso) throws ErrorServiceException {
        // La validación principal se hace en registrar()
    }
}
