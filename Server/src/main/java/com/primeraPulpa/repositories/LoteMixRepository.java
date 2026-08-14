package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.Mix;

import java.time.LocalDate;
import java.util.Optional;

public interface LoteMixRepository extends BaseRepository<LoteMix, Long> {
    Optional<LoteMix> findByMixAndFechaElaboracion(Mix mix, LocalDate fecha);
}
