package com.primeraPulpa.Services;

import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService extends BaseService<Pedido, Long> {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final MixRepository mixRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final HistorialEstadoPedidoRepository historialRepository;

    // Mapa de transiciones válidas: estado actual → estados destino permitidos
    private static final Map<String, List<String>> TRANSICIONES_VALIDAS = Map.of(
            "PENDIENTE", List.of("PREPARADO", "CANCELADO"),
            "PREPARADO", List.of("ENTREGADO", "PENDIENTE", "CANCELADO"),
            "ENTREGADO", List.of(),
            "CANCELADO", List.of()
    );

    public PedidoService(PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         MixRepository mixRepository,
                         EstadoPedidoRepository estadoPedidoRepository,
                         HistorialEstadoPedidoRepository historialRepository) {
        super(pedidoRepository);
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.mixRepository = mixRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.historialRepository = historialRepository;
    }

    @Override
    protected void validar(Pedido pedido) throws ErrorServiceException {
        if (pedido.getCliente() == null) {
            throw new ErrorServiceException("Debe indicar el cliente del pedido");
        }
        if (pedido.getUsuario() == null) {
            throw new ErrorServiceException("Debe indicar el usuario que registra el pedido");
        }
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new ErrorServiceException("El pedido debe tener al menos un mix solicitado");
        }
    }

    @Override
    protected void preAlta(Pedido pedido) throws ErrorServiceException {
        if (pedido.getFecha() == null) {
            pedido.setFecha(LocalDate.now());
        }
    }

    /**
     * Registra un pedido completo con sus detalles.
     * El pedido se crea en estado PENDIENTE (HU-13) con los detalles pendientes de preparación.
     * El stock de Mix se descuenta a medida que se preparan los ítems.
     */
    @Transactional
    public Pedido registrar(Pedido pedido) throws ErrorServiceException {
        if (pedido.getCliente() == null) {
            throw new ErrorServiceException("Debe indicar el cliente del pedido");
        }
        if (pedido.getUsuario() == null) {
            throw new ErrorServiceException("Debe indicar el usuario que registra el pedido");
        }
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new ErrorServiceException("El pedido debe tener al menos un mix solicitado");
        }

        // 1. Buscar estado PENDIENTE
        EstadoPedido estadoPendiente = estadoPedidoRepository.findByDescripcionIgnoreCase("PENDIENTE")
                .orElseThrow(() -> new ErrorServiceException("Estado PENDIENTE no encontrado en el sistema"));
        pedido.setEstadoPedido(estadoPendiente);

        // 2. Setear fecha si no viene
        if (pedido.getFecha() == null) {
            pedido.setFecha(LocalDate.now());
        }

        // 3. Validar que cada detalle tenga mix y cantidad válida
        for (DetallePedido detalle : pedido.getDetalles()) {
            if (detalle.getMix() == null) {
                throw new ErrorServiceException("Cada detalle debe indicar el mix solicitado");
            }
            if (detalle.getCantidad() <= 0) {
                throw new ErrorServiceException("La cantidad del mix '" + detalle.getMix().getNombre() + "' debe ser mayor a cero");
            }
            mixRepository.findById(detalle.getMix().getId())
                    .orElseThrow(() -> new ErrorServiceException("Mix no encontrado: " + detalle.getMix().getNombre()));
        }

        // 4. Persistir cabecera
        List<DetallePedido> detallesRef = pedido.getDetalles();
        pedido.setDetalles(new ArrayList<>());
        pedido.setEliminado(false);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 5. Guardar detalles con preparado = false (el stock se descuenta al preparar cada ítem)
        for (DetallePedido detalle : detallesRef) {
            detalle.setPedido(pedidoGuardado);
            detalle.setPreparado(false);
            detalle.setEliminado(false);
            detallePedidoRepository.save(detalle);
        }

        // 6. Registrar en historial de estados
        registrarHistorial(pedidoGuardado, estadoPendiente, pedido.getUsuario());

        return pedidoGuardado;
    }

    /**
     * Marca un detalle individual del pedido como preparado.
     * Valida y descuenta el stock de Mix en ese instante.
     * Si todos los ítems del pedido quedan preparados, cambia automáticamente el estado del pedido a PREPARADO.
     */
    @Transactional
    public void prepararDetalle(Long pedidoId, Long detalleId, Usuario usuario) throws ErrorServiceException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Pedido no encontrado"));

        String estadoActual = pedido.getEstadoPedido().getDescripcion().toUpperCase();
        if ("ENTREGADO".equals(estadoActual) || "CANCELADO".equals(estadoActual)) {
            throw new ErrorServiceException("No se puede modificar un pedido en estado " + estadoActual);
        }

        DetallePedido detalle = detallePedidoRepository.findById(detalleId)
                .filter(d -> !Boolean.TRUE.equals(d.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Detalle de pedido no encontrado"));

        if (!detalle.getPedido().getId().equals(pedidoId)) {
            throw new ErrorServiceException("El detalle no corresponde a este pedido");
        }

        if (Boolean.TRUE.equals(detalle.getPreparado())) {
            throw new ErrorServiceException("Este ítem ya se encuentra preparado");
        }

        Mix mix = mixRepository.findById(detalle.getMix().getId())
                .orElseThrow(() -> new ErrorServiceException("Mix no encontrado"));

        if (mix.getStock() < detalle.getCantidad()) {
            throw new ErrorServiceException("Stock insuficiente del mix '" + mix.getNombre() + "': "
                    + "se requieren " + redondear(detalle.getCantidad()) + " kg y hay " + redondear(mix.getStock()) + " kg disponibles. "
                    + "Registre una elaboración de este mix antes de marcarlo como preparado.");
        }

        // Descontar stock del mix
        mix.actualizarStock(-detalle.getCantidad());
        mixRepository.save(mix);

        detalle.setPreparado(true);
        detallePedidoRepository.save(detalle);

        // Verificar si todos los detalles del pedido quedaron preparados
        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedidoId);
        boolean todosPreparados = detalles.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getEliminado()))
                .allMatch(d -> Boolean.TRUE.equals(d.getPreparado()));

        if (todosPreparados && !"PREPARADO".equals(estadoActual)) {
            EstadoPedido estadoPreparado = estadoPedidoRepository.findByDescripcionIgnoreCase("PREPARADO")
                    .orElse(null);
            if (estadoPreparado != null) {
                pedido.setEstadoPedido(estadoPreparado);
                pedidoRepository.save(pedido);
                registrarHistorial(pedido, estadoPreparado, usuario);
            }
        }
    }

    /**
     * Desmarca un detalle preparado (revierte el descuento de stock de Mix).
     */
    @Transactional
    public void desmarcarDetalle(Long pedidoId, Long detalleId, Usuario usuario) throws ErrorServiceException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Pedido no encontrado"));

        String estadoActual = pedido.getEstadoPedido().getDescripcion().toUpperCase();
        if ("ENTREGADO".equals(estadoActual) || "CANCELADO".equals(estadoActual)) {
            throw new ErrorServiceException("No se puede modificar un pedido en estado " + estadoActual);
        }

        DetallePedido detalle = detallePedidoRepository.findById(detalleId)
                .filter(d -> !Boolean.TRUE.equals(d.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Detalle de pedido no encontrado"));

        if (!detalle.getPedido().getId().equals(pedidoId)) {
            throw new ErrorServiceException("El detalle no corresponde a este pedido");
        }

        if (!Boolean.TRUE.equals(detalle.getPreparado())) {
            return;
        }

        Mix mix = mixRepository.findById(detalle.getMix().getId())
                .orElseThrow(() -> new ErrorServiceException("Mix no encontrado"));

        // Restaurar stock del mix
        mix.actualizarStock(detalle.getCantidad());
        mixRepository.save(mix);

        detalle.setPreparado(false);
        detallePedidoRepository.save(detalle);

        // Si el pedido estaba PREPARADO, vuelve a PENDIENTE
        if ("PREPARADO".equals(estadoActual)) {
            EstadoPedido estadoPendiente = estadoPedidoRepository.findByDescripcionIgnoreCase("PENDIENTE")
                    .orElse(null);
            if (estadoPendiente != null) {
                pedido.setEstadoPedido(estadoPendiente);
                pedidoRepository.save(pedido);
                registrarHistorial(pedido, estadoPendiente, usuario);
            }
        }
    }

    /**
     * Cambia el estado de un pedido (HU-14).
     * Si pasa a PREPARADO, prepara automáticamente los detalles pendientes validando stock.
     * Si pasa a CANCELADO, restaura el stock de los mixes que estaban preparados.
     */
    @Transactional
    public void cambiarEstado(Long pedidoId, String nuevoEstadoDescripcion, Usuario usuario) throws ErrorServiceException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Pedido no encontrado"));

        String estadoActual = pedido.getEstadoPedido().getDescripcion().toUpperCase();
        List<String> destinosPermitidos = TRANSICIONES_VALIDAS.getOrDefault(estadoActual, List.of());

        if (destinosPermitidos.isEmpty()) {
            throw new ErrorServiceException("No se puede cambiar el estado de un pedido en estado " + estadoActual);
        }

        String destino = nuevoEstadoDescripcion.toUpperCase();
        if (!destinosPermitidos.contains(destino)) {
            throw new ErrorServiceException(
                    "Transición no válida: " + estadoActual + " → " + destino +
                    ". Estados permitidos: " + String.join(", ", destinosPermitidos));
        }

        EstadoPedido nuevoEstado = estadoPedidoRepository.findByDescripcionIgnoreCase(destino)
                .orElseThrow(() -> new ErrorServiceException("Estado '" + destino + "' no encontrado en el sistema"));

        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedidoId);

        // Si pasa a PREPARADO, preparar todos los detalles pendientes descontando stock
        if ("PREPARADO".equals(destino)) {
            for (DetallePedido det : detalles) {
                if (!Boolean.TRUE.equals(det.getPreparado())) {
                    Mix mix = mixRepository.findById(det.getMix().getId()).get();
                    if (mix.getStock() < det.getCantidad()) {
                        throw new ErrorServiceException("Stock insuficiente del mix '" + mix.getNombre() + "': "
                                + "se requieren " + redondear(det.getCantidad()) + " kg y hay " + redondear(mix.getStock()) + " kg disponibles.");
                    }
                    mix.actualizarStock(-det.getCantidad());
                    mixRepository.save(mix);
                    det.setPreparado(true);
                    detallePedidoRepository.save(det);
                }
            }
        }

        // Si se CANCELA, restaurar stock solo de los que estaban preparados
        if ("CANCELADO".equals(destino)) {
            for (DetallePedido detalle : detalles) {
                if (Boolean.TRUE.equals(detalle.getPreparado())) {
                    Mix mix = mixRepository.findById(detalle.getMix().getId()).get();
                    mix.actualizarStock(detalle.getCantidad());
                    mixRepository.save(mix);
                    detalle.setPreparado(false);
                    detallePedidoRepository.save(detalle);
                }
            }
        }

        pedido.setEstadoPedido(nuevoEstado);
        pedidoRepository.save(pedido);
        registrarHistorial(pedido, nuevoEstado, usuario);
    }

    private void registrarHistorial(Pedido pedido, EstadoPedido estado, Usuario usuario) {
        HistorialEstadoPedido historial = new HistorialEstadoPedido();
        historial.setPedido(pedido);
        historial.setEstadoPedido(estado);
        historial.setUsuario(usuario);
        historial.setFechaHora(LocalDateTime.now());
        historial.setEliminado(false);
        historialRepository.save(historial);
    }

    public List<HistorialEstadoPedido> obtenerHistorial(Long pedidoId) {
        return historialRepository.findByPedidoIdOrderByFechaHoraAsc(pedidoId);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }
}
