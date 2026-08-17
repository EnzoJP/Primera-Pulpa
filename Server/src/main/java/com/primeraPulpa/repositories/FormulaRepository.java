package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Formula;

import java.util.List;

public interface FormulaRepository extends BaseRepository<Formula, Long> {

    // HU-07 / HU-11: la fórmula de un mix (ahora 1 fórmula = 1 mix)
    List<Formula> findByMixId(Long mixId);
}
