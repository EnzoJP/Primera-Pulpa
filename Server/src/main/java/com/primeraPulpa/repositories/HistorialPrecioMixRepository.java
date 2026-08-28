package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.HistorialPrecioMix;

import java.util.List;

public interface HistorialPrecioMixRepository extends BaseRepository<HistorialPrecioMix, Long> {

    List<HistorialPrecioMix> findByMixIdOrderByFechaHoraDesc(Long mixId);
}
