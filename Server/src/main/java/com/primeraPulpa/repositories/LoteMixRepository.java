package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.Mix;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteMixRepository extends BaseRepository<LoteMix, Long> {
    Optional<LoteMix> findByMixAndFechaElaboracion(Mix mix, LocalDate fecha);

    Optional<LoteMix> findByMixAndFechaElaboracionAndEliminadoFalse(Mix mix, LocalDate fecha);

    List<LoteMix> findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc();

    List<LoteMix> findAllByEliminadoFalseAndFechaElaboracionOrderByIdAsc(LocalDate fecha);
}