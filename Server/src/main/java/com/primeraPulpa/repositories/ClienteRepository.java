package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Cliente;

import java.util.List;

public interface ClienteRepository extends BaseRepository<Cliente, Long> {

    // HU-12: buscar un cliente por nombre o contacto
    List<Cliente> findByNombreContainingIgnoreCaseOrContactoContainingIgnoreCase(String nombre, String contacto);

    // Para validar duplicados / búsqueda exacta por cuit
    Cliente findByCuit(String cuit);
}
