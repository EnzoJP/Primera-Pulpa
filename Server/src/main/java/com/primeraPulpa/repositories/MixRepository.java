package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Mix;

import java.util.List;

public interface MixRepository extends BaseRepository<Mix, Long> {

    // HU-10: filtrar por nombre
    List<Mix> findByNombreContainingIgnoreCase(String nombre);

    // Trazabilidad de "quién realizó" cada elaboración (relación "realiza")
    List<Mix> findByUsuarioId(Long usuarioId);
}
