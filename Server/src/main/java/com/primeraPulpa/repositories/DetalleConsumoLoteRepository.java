package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.DetalleConsumoLote;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleConsumoLoteRepository extends BaseRepository<DetalleConsumoLote, Long> {

    @Query("SELECT d FROM DetalleConsumoLote d " +
           "WHERE d.loteMix.id = :loteMixId AND (d.eliminado IS NULL OR d.eliminado = false) " +
           "ORDER BY d.materiaPrima.nombre ASC, d.lote.fechaVencimiento ASC NULLS LAST, d.lote.id ASC")
    List<DetalleConsumoLote> findByLoteMixId(@Param("loteMixId") Long loteMixId);
}