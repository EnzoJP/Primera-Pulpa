package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.DetallePedido;

import java.util.List;

public interface DetallePedidoRepository extends BaseRepository<DetallePedido, Long> {

    // Detalles de un pedido puntual
    List<DetallePedido> findByPedidoId(Long pedidoId);

    // HU-16: historial de movimientos de StockMix de un mix (lado "egreso" por pedidos)
    List<DetallePedido> findByMixId(Long mixId);

    // Cantidad total pendiente de preparación por mix (solo ítems NO preparados en pedidos PENDIENTE)
    @org.springframework.data.jpa.repository.Query(
        "SELECT dp.mix.id, SUM(dp.cantidad) FROM DetallePedido dp " +
        "JOIN dp.pedido p " +
        "WHERE p.eliminado = false AND p.estadoPedido.descripcion = 'PENDIENTE' " +
        "AND dp.eliminado = false " +
        "AND (dp.preparado = false OR dp.preparado IS NULL) " +
        "GROUP BY dp.mix.id")
    List<Object[]> sumCantidadPendienteByMixId();
}
