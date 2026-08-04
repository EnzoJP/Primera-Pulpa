package com.primeraPulpa.repositories;

import com.primeraPulpa.entities.Pedido;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PedidoRepository extends BaseRepository<Pedido, Long> {

    // Dashboard de cliente / HU-13
    List<Pedido> findByClienteId(Long clienteId);

    // HU-14: pedidos por estado (ej. todos los PENDIENTE)
    List<Pedido> findByEstadoPedidoId(Long estadoPedidoId);

    // Trazabilidad: pedidos registrados por un usuario
    List<Pedido> findByUsuarioId(Long usuarioId);

    // HU-16: historial de pedidos en un rango de fechas
    @Query("SELECT p FROM Pedido p " +
           "WHERE p.eliminado = false " +
           "AND p.fecha BETWEEN :desde AND :hasta")
    List<Pedido> findByFechaBetween(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
