package com.primeraPulpa.controller;

import com.primeraPulpa.Services.ClienteService;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.Services.PedidoService;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.repositories.EstadoPedidoRepository;
import com.primeraPulpa.repositories.PedidoRepository;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private static final int PAGE_SIZE = 30;

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final ClienteService clienteService;
    private final MixService mixService;
    private final UsuarioRepository usuarioRepository;

    public PedidoController(PedidoService pedidoService,
                            PedidoRepository pedidoRepository,
                            DetallePedidoRepository detallePedidoRepository,
                            EstadoPedidoRepository estadoPedidoRepository,
                            ClienteService clienteService,
                            MixService mixService,
                            UsuarioRepository usuarioRepository) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.clienteService = clienteService;
        this.mixService = mixService;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Listado con filtro por fecha, estado y paginación ────────────────────
    @GetMapping("")
    public String listado(@RequestParam(value = "fecha", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @RequestParam(value = "estado", required = false) String estado,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          Model model) {
        List<Pedido> todos = pedidoRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .sorted((a, b) -> {
                    int cmp = b.getFecha().compareTo(a.getFecha());
                    return cmp != 0 ? cmp : Long.compare(b.getId(), a.getId());
                })
                .filter(p -> fecha == null || p.getFecha().equals(fecha))
                .filter(p -> estado == null || estado.isBlank()
                        || (p.getEstadoPedido() != null && p.getEstadoPedido().getDescripcion() != null
                            && p.getEstadoPedido().getDescripcion().equalsIgnoreCase(estado)))
                .toList();

        int total = todos.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int paginaActual = Math.max(0, Math.min(page, totalPages - 1));
        int desde = paginaActual * PAGE_SIZE;
        int hasta = Math.min(desde + PAGE_SIZE, total);
        List<Pedido> pagina = desde < total ? todos.subList(desde, hasta) : List.of();

        Map<Long, Double> totalPorPedido = new java.util.HashMap<>();
        for (Pedido p : pagina) {
            double suma = 0;
            if (p.getDetalles() != null) {
                for (DetallePedido d : p.getDetalles()) {
                    if (d != null) {
                        suma += d.getCantidad() * d.getPrecioUnitario();
                    }
                }
            }
            totalPorPedido.put(p.getId(), suma);
        }

        model.addAttribute("items", pagina);
        model.addAttribute("totalPorPedido", totalPorPedido);
        model.addAttribute("fechaFiltro", fecha);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("estadosPosibles", List.of("PENDIENTE", "PREPARADO", "ENTREGADO", "CANCELADO"));
        model.addAttribute("page", paginaActual);
        model.addAttribute("totalPages", totalPages);
        return "pedidos/list";
    }

    // ── Formulario nuevo ─────────────────────────────────────────────────────
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDate.now());
        model.addAttribute("pedido", pedido);
        cargarDatosFormulario(model);
        model.addAttribute("formTitle", "Nuevo Pedido");
        model.addAttribute("formAction", "/pedidos/registrar");
        return "pedidos/form";
    }

    // ── Confirmar pedido ─────────────────────────────────────────────────────
    @PostMapping("/registrar")
    public String registrar(
            @RequestParam("clienteId") Long clienteId,
            @RequestParam("mixId") List<Long> mixIds,
            @RequestParam("cantidad") List<Double> cantidades,
            @RequestParam("precioUnitario") List<Double> preciosUnitarios,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {

        try {
            Cliente cliente = clienteService.getOne(clienteId);
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));

            List<DetallePedido> detalles = new ArrayList<>();
            for (int i = 0; i < mixIds.size(); i++) {
                if (mixIds.get(i) == null || mixIds.get(i) == 0) continue;

                Mix mix = mixService.getOne(mixIds.get(i));

                DetallePedido detalle = new DetallePedido();
                detalle.setMix(mix);
                detalle.setCantidad(cantidades.get(i));
                detalle.setPrecioUnitario(preciosUnitarios.get(i));
                detalles.add(detalle);
            }

            if (detalles.isEmpty()) {
                throw new ErrorServiceException("El pedido debe incluir al menos un mix");
            }

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setUsuario(usuario);
            pedido.setDetalles(detalles);

            pedidoService.registrar(pedido);
            redirectAttributes.addFlashAttribute("success", "Pedido registrado correctamente en estado PENDIENTE.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el pedido: " + e.getMessage());
        }
        return "redirect:/pedidos";
    }

    // ── Ver detalle de un pedido ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        try {
            Pedido pedido = pedidoService.getOne(id);
            List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(id);
            double total = detalles.stream()
                    .mapToDouble(d -> d.getCantidad() * d.getPrecioUnitario())
                    .sum();

            // Historial de cambios de estado (HU-14)
            List<HistorialEstadoPedido> historial = pedidoService.obtenerHistorial(id);

            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", detalles);
            model.addAttribute("totalPedido", total);
            model.addAttribute("historial", historial);
            return "pedidos/detail";
        } catch (ErrorServiceException e) {
            model.addAttribute("error", "Pedido no encontrado");
            return "error/404";
        }
    }

    // ── Preparar ítem individual del pedido ─────────────────────────────────
    @PostMapping("/{pedidoId}/detalles/{detalleId}/preparar")
    public String prepararDetalle(@PathVariable Long pedidoId,
                                  @PathVariable Long detalleId,
                                  @AuthenticationPrincipal User user,
                                  RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));
            pedidoService.prepararDetalle(pedidoId, detalleId, usuario);
            redirectAttributes.addFlashAttribute("success", "Ítem preparado exitosamente. Se descontó el stock de mix.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar el ítem: " + e.getMessage());
        }
        return "redirect:/pedidos/" + pedidoId;
    }

    // ── Desmarcar ítem preparado (revierte descuento de stock) ──────────────
    @PostMapping("/{pedidoId}/detalles/{detalleId}/desmarcar")
    public String desmarcarDetalle(@PathVariable Long pedidoId,
                                   @PathVariable Long detalleId,
                                   @AuthenticationPrincipal User user,
                                   RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));
            pedidoService.desmarcarDetalle(pedidoId, detalleId, usuario);
            redirectAttributes.addFlashAttribute("success", "Se desmarcó el ítem y se restauró el stock de mix.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al desmarcar el ítem: " + e.getMessage());
        }
        return "redirect:/pedidos/" + pedidoId;
    }

    // ── Cambiar estado del pedido (HU-14) ────────────────────────────────────
    @PostMapping("/{id}/cambiar-estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam("nuevoEstado") String nuevoEstado,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));

            pedidoService.cambiarEstado(id, nuevoEstado, usuario);
            redirectAttributes.addFlashAttribute("success",
                    "Estado actualizado correctamente a " + nuevoEstado.toUpperCase());
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar el estado: " + e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    // ── Eliminar pedido ─────────────────────────────────────────────────────
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pedidoService.bajaLogica(id);
            redirectAttributes.addFlashAttribute("success", "Pedido eliminado correctamente.");
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el pedido.");
        }
        return "redirect:/pedidos";
    }

    // ── API: Pedidos de un cliente ───────────────────────────────────────────
    @ResponseBody
    @GetMapping("/cliente/{clienteId}")
    public List<Pedido> getByCliente(@PathVariable Long clienteId) {
        try {
            return pedidoRepository.findByClienteId(clienteId);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    // ── API: Stock actual de un mix ──────────────────────────────────────────
    @ResponseBody
    @GetMapping("/mix/{mixId}/stock")
    public java.util.Map<String, Object> getStockMix(@PathVariable Long mixId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Mix mix = mixService.getOne(mixId);
            result.put("stock", mix.getStock());
            result.put("precioVenta", mix.getPrecioVenta());
            result.put("nombre", mix.getNombre());
        } catch (Exception e) {
            result.put("stock", 0);
            result.put("error", "Mix no encontrado");
        }
        return result;
    }

    private void cargarDatosFormulario(Model model) {
        try {
            model.addAttribute("clientes", clienteService.listarActivos());
        } catch (ErrorServiceException e) {
            model.addAttribute("clientes", List.of());
        }
        try {
            model.addAttribute("mixes", mixService.listarActivos().stream()
                    .sorted(Mix.ordenarPorPresentacion())
                    .toList());
            model.addAttribute("cantidadesPendientes", mixService.cantidadesPendientesPorMix());
        } catch (ErrorServiceException e) {
            model.addAttribute("mixes", List.of());
        }
    }
}
