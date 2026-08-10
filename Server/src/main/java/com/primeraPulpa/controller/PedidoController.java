package com.primeraPulpa.controller;

import com.primeraPulpa.Services.ClienteService;
import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.repositories.PedidoRepository;
import com.primeraPulpa.Services.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/pedidos")
public class PedidoController extends BaseController<Pedido, Long> {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final MixService mixService;

    public PedidoController(PedidoService service, PedidoRepository pedidoRepository, ClienteService clienteService, MixService mixService) {
        super(service, Pedido.class, "/api/v1/pedidos", "pedido");
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.mixService = mixService;
    }

    // Dashboard de cliente / HU-13
    @ResponseBody
    @GetMapping("/cliente/{clienteId}")
    public java.util.List<Pedido> getByCliente(@PathVariable Long clienteId) {
        try {
            return pedidoRepository.findByClienteId(clienteId);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("clientes", clienteService.listarActivos());
        model.addAttribute("mixes", mixService.listarActivos());
    }

    @Override
    protected String vistaFormulario() {
        return "pedidos/form";  // template propio con selects reales
    }
}
