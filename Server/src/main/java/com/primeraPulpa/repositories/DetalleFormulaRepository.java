package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.DetalleFormula;

import java.util.List;

public interface DetalleFormulaRepository extends BaseRepository<DetalleFormula, Long> {

    // Para validar que una materia prima no esté en ninguna fórmula y para recalcular costos
    List<DetalleFormula> findByMateriaPrimaId(Long materiaPrimaId);
}