package com.primeraPulpa.Services;

import com.primeraPulpa.dto.LoteDiarioDTO;
import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetalleFormulaRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MateriaPrimaService extends BaseService<MateriaPrima, Long> {

    private final MateriaPrimaRepository repository;
    private final DetalleFormulaRepository detalleFormulaRepository;
    private final MixService mixService;

    public MateriaPrimaService(MateriaPrimaRepository repository, DetalleFormulaRepository detalleFormulaRepository,
                               MixService mixService) {
        super(repository);
        this.repository = repository;
        this.detalleFormulaRepository = detalleFormulaRepository;
        this.mixService = mixService;
    }

    @Override
    protected void validar(MateriaPrima materiaPrima) throws ErrorServiceException {
        if (materiaPrima.getNombre() == null || materiaPrima.getNombre().trim().isEmpty()) {
            throw new ErrorServiceException("Debe indicar el nombre de la materia prima");
        }
        if (materiaPrima.getUnidadMedida() == null) {
            throw new ErrorServiceException("Debe indicar la unidad de medida");
        }
        if (materiaPrima.getPrecio() < 0) {
            throw new ErrorServiceException("El precio no puede ser negativo");
        }
        if (materiaPrima.getCantidadMinima() < 0) {
            throw new ErrorServiceException("El stock mínimo no puede ser negativo");
        }
    }

    @Override
    protected void preAlta(MateriaPrima materiaPrima) throws ErrorServiceException {
        // HU-03: el stock siempre arranca en 0, sin importar lo que venga en el request
        materiaPrima.setCantidadActual(0);
        if (materiaPrima.getFechaIngreso() == null) {
            materiaPrima.setFechaIngreso(LocalDate.now());
        }
    }

    // El formulario de edición no incluye stock, fecha de ingreso ni estado de baja:
    // se conservan los valores persistidos para que la edición no los pise (ej. el stock pasaba a 0).
    @Override
    public Optional<MateriaPrima> modificar(Long id, MateriaPrima entidadNueva) throws ErrorServiceException {
        try {
            validar(entidadNueva);
            entidadNueva.setId(id);
            preModificacion(entidadNueva);
            return repository.findById(id).map(entidad -> {
                entidadNueva.setCantidadActual(entidad.getCantidadActual());
                entidadNueva.setFechaIngreso(entidad.getFechaIngreso());
                entidadNueva.setEliminado(entidad.getEliminado());
                MateriaPrima actualizado = repository.save(entidadNueva);
                postModificacion(actualizado);
                return actualizado;
            });
        } catch (Exception e) {
            throw new ErrorServiceException("Error de Sistemas");
        }
    }

    // Si cambia el precio, se recalculan los costos de los mixes que la usan
    @Override
    protected void postModificacion(MateriaPrima materiaPrima) throws ErrorServiceException {
        detalleFormulaRepository.findByMateriaPrimaId(materiaPrima.getId()).stream()
                .map(d -> d.getFormula().getMix().getId())
                .distinct()
                .forEach(mixService::recalcularCosto);
    }

    // HU-03: no se puede dar de baja una materia prima usada en una fórmula
    @Override
    protected void preBaja(Long id) throws ErrorServiceException {
        boolean referenciada = !detalleFormulaRepository.findByMateriaPrimaId(id).isEmpty();
        if (referenciada) {
            throw new ErrorServiceException("No se puede dar de baja una materia prima utilizada en una fórmula");
        }
    }

}
