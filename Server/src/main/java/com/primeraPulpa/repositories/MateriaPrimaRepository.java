package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.MateriaPrima;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MateriaPrimaRepository extends BaseRepository<MateriaPrima, Long> {

    // HU-05: filtrar por nombre
    List<MateriaPrima> findByNombreContainingIgnoreCase(String nombre);

    List<MateriaPrima> findByUnidadMedidaId(Long id);

    // HU-06: materias primas cuyo stock actual está por debajo del mínimo
    @Query("SELECT m FROM MateriaPrima m " +
           "WHERE m.eliminado = false " +
           "AND m.cantidadActual < m.cantidadMinima")
    List<MateriaPrima> findConStockBajo();

    @Query("SELECT CASE WHEN COUNT(df) > 0 THEN true ELSE false END FROM DetalleFormula df WHERE df.materiaPrima.id = :id AND df.eliminado = false")
    Boolean findInFormulaById(Long id);
}
