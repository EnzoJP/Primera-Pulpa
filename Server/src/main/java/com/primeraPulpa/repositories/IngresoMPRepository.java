package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.IngresoMP;

import java.time.LocalDateTime;
import java.util.List;

public interface IngresoMPRepository extends BaseRepository<IngresoMP, Long> {

    // Histórico de ingresos: más recientes primero
    List<IngresoMP> findAllByEliminadoFalseOrderByFechaHoraDescIdDesc();

    List<IngresoMP> findAllByEliminadoFalseAndFechaHoraBetweenOrderByFechaHoraAscIdAsc(LocalDateTime desde, LocalDateTime hasta);
}
