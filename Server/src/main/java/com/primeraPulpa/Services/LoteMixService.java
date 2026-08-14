package com.primeraPulpa.Services;

import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.repositories.LoteMixRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class LoteMixService extends BaseService<LoteMix, Long> {

    private final LoteMixRepository loteMixRepository;

    public LoteMixService(LoteMixRepository repository) {
        super(repository);
        this.loteMixRepository = repository;
    }

    @Transactional
    public LoteMix registrarElaboracion(Mix mix, Double cantidad) {
        LocalDate hoy = LocalDate.now();

        // Buscar si ya existe un lote de este mix para hoy
        Optional<LoteMix> loteExistente = loteMixRepository
                .findByMixAndFechaElaboracion(mix, hoy);

        if (loteExistente.isPresent()) {
            // Ya hay un lote hoy → sumar cantidad
            LoteMix lote = loteExistente.get();
            double actual = lote.getCantidadElaborada() != null ? lote.getCantidadElaborada() : 0.0;
            lote.setCantidadElaborada(actual + cantidad);
            return loteMixRepository.save(lote);
        } else {
            // No hay lote hoy → crear uno nuevo
            LoteMix lote = new LoteMix();
            lote.setMix(mix);
            lote.setFechaElaboracion(hoy);
            lote.setCantidadElaborada(cantidad);
            return loteMixRepository.save(lote);
        }
    }
}
