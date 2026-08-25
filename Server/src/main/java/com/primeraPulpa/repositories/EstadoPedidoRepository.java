package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.EstadoPedido;

import java.util.Optional;

public interface EstadoPedidoRepository extends BaseRepository<EstadoPedido, Long> {

    Optional<EstadoPedido> findByDescripcionIgnoreCase(String descripcion);
}
