package com.primeraPulpa.Services;

import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.entities.EstadoPedido;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.repositories.EstadoPedidoRepository;
import com.primeraPulpa.repositories.MixRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService extends BaseService<Pedido, Long> {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final MixRepository mixRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         MixRepository mixRepository,
                         EstadoPedidoRepository estadoPedidoRepository) {
        super(pedidoRepository);
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.mixRepository = mixRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
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
     * Registra un pedido completo con sus detalles, valida stock y descuenta del Mix.
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

        // 3. Validar stock de cada mix antes de descontar nada (all-or-nothing)
        for (DetallePedido detalle : pedido.getDetalles()) {
            if (detalle.getMix() == null) {
                throw new ErrorServiceException("Cada detalle debe indicar el mix solicitado");
            }
            if (detalle.getCantidad() <= 0) {
                throw new ErrorServiceException("La cantidad del mix '" + detalle.getMix().getNombre() + "' debe ser mayor a cero");
            }

            Mix mix = mixRepository.findById(detalle.getMix().getId())
                    .orElseThrow(() -> new ErrorServiceException("Mix no encontrado: " + detalle.getMix().getNombre()));

            if (mix.getStock() < detalle.getCantidad()) {
                throw new ErrorServiceException(
                        "Stock insuficiente de '" + mix.getNombre()
                        + "': se solicitan " + redondear(detalle.getCantidad()) + " kg pero hay "
                        + redondear(mix.getStock()) + " kg disponibles");
            }
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

    private static double redondear(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }
}
