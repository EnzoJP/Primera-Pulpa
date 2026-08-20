package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.DetalleIngresoMP;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleIngresoMPRepository extends BaseRepository<DetalleIngresoMP, Long> {

    List<DetalleIngresoMP> findByIngresoMPId(Long ingresoMPId);

    @Query("SELECT d FROM DetalleIngresoMP d WHERE d.materiaPrima.id = :mpId AND (d.eliminado IS NULL OR d.eliminado = false)")
    List<DetalleIngresoMP> findByMateriaPrimaId(@Param("mpId") Long mpId);

    // Lotes disponibles ordenados FEFO: el que vence antes se consume primero;
    // los lotes sin vencimiento al final, y entre iguales el ingreso más viejo primero.
    @Query("SELECT d FROM DetalleIngresoMP d " +
           "WHERE d.materiaPrima.id = :mpId AND (d.eliminado IS NULL OR d.eliminado = false) " +
           "ORDER BY d.fechaVencimiento ASC NULLS LAST, d.ingresoMP.fechaHora ASC, d.id ASC")
    List<DetalleIngresoMP> findLotesDisponiblesFEFO(@Param("mpId") Long mpId);
}
