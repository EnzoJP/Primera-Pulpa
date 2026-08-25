package com.primeraPulpa.controller;

import com.primeraPulpa.Services.ClienteService;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.Services.PedidoService;
import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetallePedidoRepository;
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

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private static final int PAGE_SIZE = 30;

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ClienteService clienteService;
    private final MixService mixService;
    private final UsuarioRepository usuarioRepository;

    public PedidoController(PedidoService pedidoService,
                            PedidoRepository pedidoRepository,
                            DetallePedidoRepository detallePedidoRepository,
                            ClienteService clienteService,
                            MixService mixService,
                            UsuarioRepository usuarioRepository) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.clienteService = clienteService;
        this.mixService = mixService;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Listado con filtro por fecha y paginación ─────────────────────────────
    @GetMapping("")
    public String listado(@RequestParam(value = "fecha", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          Model model) {
        List<Pedido> todos = pedidoRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .sorted((a, b) -> {
                    int cmp = b.getFecha().compareTo(a.getFecha());
                    return cmp != 0 ? cmp : Long.compare(b.getId(), a.getId());
                })
                .filter(p -> fecha == null || p.getFecha().equals(fecha))
                .toList();

        int total = todos.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int paginaActual = Math.max(0, Math.min(page, totalPages - 1));
        int desde = paginaActual * PAGE_SIZE;
        int hasta = Math.min(desde + PAGE_SIZE, total);
        List<Pedido> pagina = desde < total ? todos.subList(desde, hasta) : List.of();

        model.addAttribute("items", pagina);
        model.addAttribute("fechaFiltro", fecha);
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
            // 1. Buscar cliente
            Cliente cliente = clienteService.getOne(clienteId);

            // 2. Buscar usuario autenticado
            Usuario usuario = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ErrorServiceException("Usuario no encontrado"));

            // 3. Construir detalles
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

            // 4. Construir pedido y registrar
            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setUsuario(usuario);
            pedido.setDetalles(detalles);

            pedidoService.registrar(pedido);
            redirectAttributes.addFlashAttribute("success", "Pedido registrado correctamente. Stock de mixes actualizado.");
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
            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", detalles);
            model.addAttribute("totalPedido", total);
            return "pedidos/detail";
        } catch (ErrorServiceException e) {
            model.addAttribute("error", "Pedido no encontrado");
            return "error/404";
        }
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

    // ── API: Stock actual de un mix (para validación en JS) ──────────────────
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
            model.addAttribute("mixes", mixService.listarActivos());
        } catch (ErrorServiceException e) {
            model.addAttribute("mixes", List.of());
        }
    }
}
