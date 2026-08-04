package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.DetallePedido;

import java.util.List;

public interface DetallePedidoRepository extends BaseRepository<DetallePedido, Long> {

    // Detalles de un pedido puntual
    List<DetallePedido> findByPedidoId(Long pedidoId);

    // HU-16: historial de movimientos de StockMix de un mix (lado "egreso" por pedidos)
    List<DetallePedido> findByMixId(Long mixId);
}
