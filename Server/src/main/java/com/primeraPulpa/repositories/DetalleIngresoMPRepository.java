package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.DetalleIngresoMP;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleIngresoMPRepository extends BaseRepository<DetalleIngresoMP, Long> {

    List<DetalleIngresoMP> findByIngresoMPId(Long ingresoMPId);

    @Query("SELECT d FROM DetalleIngresoMP d WHERE d.materiaPrima.id = :mpId AND (d.eliminado IS NULL OR d.eliminado = false)")
    List<DetalleIngresoMP> findByMateriaPrimaId(@Param("mpId") Long mpId);
}
