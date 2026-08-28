package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Formula;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormulaRepository extends BaseRepository<Formula, Long> {

    // HU-07 / HU-11: la fórmula de un mix (ahora 1 fórmula = 1 mix)
    @Query("SELECT f FROM Formula f WHERE f.mix.id = :mixId")
    List<Formula> findByMixId(@Param("mixId") Long mixId);
}
