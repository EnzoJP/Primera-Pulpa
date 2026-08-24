package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.Services.ClienteService;
import com.primeraPulpa.repositories.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(path = "/clientes")
public class ClienteController extends BaseController<Cliente, Long> {

    private final ClienteService clienteService;
    private final PedidoRepository pedidoRepository;

    public ClienteController(ClienteService service, PedidoRepository pedidoRepository) {
        super(service, Cliente.class, "/clientes", "cliente");
        this.clienteService = service;
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping("")
    @Override
    public String getAll(@RequestParam Map<String, String> params, Model model) {
        try {
            String buscar = params.get("buscar");
            List<Cliente> items;
            if (buscar != null && !buscar.trim().isEmpty()) {
                items = clienteService.buscar(buscar);
                model.addAttribute("buscar", buscar.trim());
            } else {
                items = clienteService.listarActivos();
            }

            cargarAtributosBase(model);
            model.addAttribute("items", items);
            return vistaListado();
        } catch (Exception e) {
            model.addAttribute("error", "Error al listar los clientes");
            return "error/500";
        }
    }

    @GetMapping("/{id}")
    @Override
    public String getOne(@PathVariable Long id, Model model) {
        try {
            Cliente cliente = service.getOne(id);
            cargarAtributosBase(model);
            model.addAttribute("item", cliente);

            // Cargar pedidos asociados al cliente (HU-12 / HU-13)
            List<Pedido> pedidos = pedidoRepository.findByClienteId(id);
            model.addAttribute("pedidos", pedidos);

            return vistaDetalle();
        } catch (Exception e) {
            model.addAttribute("error", "Cliente no encontrado");
            return "error/404";
        }
    }

    @PostMapping("/nuevo")
    public String createAlias(@Valid @ModelAttribute("item") Cliente entidad,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        return create(entidad, bindingResult, redirectAttributes, model);
    }

    @PostMapping("/{id}/editar")
    public String updateAlias(@PathVariable Long id,
                              @Valid @ModelAttribute("item") Cliente entidad,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        return update(id, entidad, bindingResult, redirectAttributes, model);
    }

    @Override
    protected String vistaListado() {
        return "cliente/list";
    }

    @Override
    protected String vistaFormulario() {
        return "cliente/form";
    }

    @Override
    protected String vistaDetalle() {
        return "cliente/detail";
    }
}
