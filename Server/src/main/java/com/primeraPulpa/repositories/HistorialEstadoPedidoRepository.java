package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.HistorialEstadoPedido;

import java.util.List;

public interface HistorialEstadoPedidoRepository extends BaseRepository<HistorialEstadoPedido, Long> {

    List<HistorialEstadoPedido> findByPedidoIdOrderByFechaHoraAsc(Long pedidoId);
}
