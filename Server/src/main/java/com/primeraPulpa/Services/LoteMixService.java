package com.primeraPulpa.Services;

import com.primeraPulpa.dto.LoteDiarioDTO;
import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.LoteMixRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoteMixService extends BaseService<LoteMix, Long> {

    private final LoteMixRepository loteMixRepository;

    public LoteMixService(LoteMixRepository repository) {
        super(repository);
        this.loteMixRepository = repository;
    }

    /**
     * Registra una elaboración. El "lote del día" se compone de todos los LoteMix
     * de una misma fecha. Si ya existe un LoteMix del mismo mix para esa fecha,
     * se suma la cantidad al existente; si no, se crea uno nuevo.
     */
    @Transactional
    public LoteMix registrarElaboracion(Mix mix, LocalDate fecha, Double cantidad) throws ErrorServiceException {
        if (mix == null || mix.getId() == null) {
            throw new ErrorServiceException("Debe indicar el mix elaborado.");
        }
        if (fecha == null) {
            throw new ErrorServiceException("Debe indicar la fecha de elaboración.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }

        Optional<LoteMix> loteExistente = loteMixRepository
                .findByMixAndFechaElaboracionAndEliminadoFalse(mix, fecha);

        if (loteExistente.isPresent()) {
            LoteMix lote = loteExistente.get();
            double actual = lote.getCantidadElaborada() != null ? lote.getCantidadElaborada() : 0.0;
            lote.setCantidadElaborada(actual + cantidad);
            return loteMixRepository.save(lote);
        } else {
            LoteMix lote = new LoteMix();
            lote.setMix(mix);
            lote.setFechaElaboracion(fecha);
            lote.setCantidadElaborada(cantidad);
            lote.setEliminado(false);
            return loteMixRepository.save(lote);
        }
    }

    /**
     * Agrupa las elaboraciones activas por fecha (lote del día), de más reciente a más antigua.
     */
    public List<LoteDiarioDTO> listarLotesPorDia() {
        return loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc()
                .stream()
                .collect(Collectors.groupingBy(LoteMix::getFechaElaboracion,
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new LoteDiarioDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Todos los LoteMix activos de una fecha determinada (el lote del día completo).
     */
    public List<LoteMix> listarDelDia(LocalDate fecha) {
        return loteMixRepository.findAllByEliminadoFalseAndFechaElaboracionOrderByIdAsc(fecha);
    }

    @Override
    protected void validar(LoteMix lote) throws ErrorServiceException {
        if (lote.getMix() == null) {
            throw new ErrorServiceException("Debe indicar el mix elaborado.");
        }
        if (lote.getFechaElaboracion() == null) {
            throw new ErrorServiceException("Debe indicar la fecha de elaboración.");
        }
        if (lote.getCantidadElaborada() == null || lote.getCantidadElaborada() <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }
    }
}