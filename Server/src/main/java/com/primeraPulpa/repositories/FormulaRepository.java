package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Formula;

import java.util.List;

public interface FormulaRepository extends BaseRepository<Formula, Long> {

    // HU-07 / HU-11: todas las fórmulas (componentes) de un mix, para calcular consumo y costo
    List<Formula> findByMixId(Long mixId);

    // HU-03: para validar que una materia prima no esté referenciada en una fórmula antes de darla de baja
    List<Formula> findByMateriaPrimaId(Long materiaPrimaId);
}
