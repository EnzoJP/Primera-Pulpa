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
            "PREPARADO", List.of("ENTREGADO", "CANCELADO"),
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
     * Registra un pedido completo con sus detalles, descuenta stock del Mix.
     * El pedido se crea en estado PENDIENTE (HU-13).
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

        // 4. Persistir cabecera (sin detalles para evitar cascade con referencias nulas)
        List<DetallePedido> detallesRef = pedido.getDetalles();
        pedido.setDetalles(new ArrayList<>());
        pedido.setEliminado(false);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 5. Guardar detalles y descontar stock del Mix
        for (DetallePedido detalle : detallesRef) {
            Mix mix = mixRepository.findById(detalle.getMix().getId()).get();

            detalle.setPedido(pedidoGuardado);
            detalle.setEliminado(false);
            detallePedidoRepository.save(detalle);

            // Descontar stock del Mix
            mix.actualizarStock(-detalle.getCantidad());
            mixRepository.save(mix);
        }

        return pedidoGuardado;
    }

    /**
     * Cambia el estado de un pedido (HU-14).
     * Valida la transición, registra en el historial y restaura stock si se cancela.
     */
    @Transactional
    public void cambiarEstado(Long pedidoId, String nuevoEstadoDescripcion, Usuario usuario) throws ErrorServiceException {
        // 1. Buscar el pedido
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .orElseThrow(() -> new ErrorServiceException("Pedido no encontrado"));

        // 2. Verificar que el pedido no esté en estado final
        String estadoActual = pedido.getEstadoPedido().getDescripcion().toUpperCase();
        List<String> destinosPermitidos = TRANSICIONES_VALIDAS.getOrDefault(estadoActual, List.of());

        if (destinosPermitidos.isEmpty()) {
            throw new ErrorServiceException("No se puede cambiar el estado de un pedido en estado " + estadoActual);
        }

        // 3. Verificar que el nuevo estado sea válido para la transición
        String destino = nuevoEstadoDescripcion.toUpperCase();
        if (!destinosPermitidos.contains(destino)) {
            throw new ErrorServiceException(
                    "Transición no válida: " + estadoActual + " → " + destino +
                    ". Estados permitidos: " + String.join(", ", destinosPermitidos));
        }

        // 4. Buscar el nuevo estado
        EstadoPedido nuevoEstado = estadoPedidoRepository.findByDescripcionIgnoreCase(destino)
                .orElseThrow(() -> new ErrorServiceException("Estado '" + destino + "' no encontrado en el sistema"));

        // 5. Si se cancela, restaurar stock de los mixes
        if ("CANCELADO".equals(destino)) {
            List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedidoId);
            for (DetallePedido detalle : detalles) {
                Mix mix = mixRepository.findById(detalle.getMix().getId()).get();
                mix.actualizarStock(detalle.getCantidad());
                mixRepository.save(mix);
            }
        }

        // 6. Actualizar el estado del pedido
        pedido.setEstadoPedido(nuevoEstado);
        pedidoRepository.save(pedido);

        // 7. Registrar en el historial
        HistorialEstadoPedido historial = new HistorialEstadoPedido();
        historial.setPedido(pedido);
        historial.setEstadoPedido(nuevoEstado);
        historial.setUsuario(usuario);
        historial.setFechaHora(LocalDateTime.now());
        historial.setEliminado(false);
        historialRepository.save(historial);
    }

    /**
     * Obtiene el historial de estados de un pedido.
     */
    public List<HistorialEstadoPedido> obtenerHistorial(Long pedidoId) {
        return historialRepository.findByPedidoIdOrderByFechaHoraAsc(pedidoId);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }
}
