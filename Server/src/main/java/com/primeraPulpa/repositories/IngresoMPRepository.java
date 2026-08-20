package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.IngresoMP;

import java.util.List;

public interface IngresoMPRepository extends BaseRepository<IngresoMP, Long> {

    // Histórico de ingresos: más recientes primero
    List<IngresoMP> findAllByEliminadoFalseOrderByFechaHoraDescIdDesc();
}
