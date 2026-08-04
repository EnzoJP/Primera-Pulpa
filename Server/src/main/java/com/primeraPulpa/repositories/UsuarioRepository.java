package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends BaseRepository<Usuario, Long> {

    // HU-01: login por email
    Optional<Usuario> findByEmail(String email);

    // Definido en el diagrama: buscarPorNombre
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);
}
