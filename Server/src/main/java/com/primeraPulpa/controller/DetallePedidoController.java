package com.primeraPulpa.controller;

import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.Services.DetallePedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/detalles-pedido")
public class DetallePedidoController extends BaseController<DetallePedido, Long> {

    private final DetallePedidoRepository detallePedidoRepository;

    public DetallePedidoController(DetallePedidoService service, DetallePedidoRepository detallePedidoRepository) {
        super(service, DetallePedido.class, "/api/v1/detalles-pedido", "detallePedido");
        this.detallePedidoRepository = detallePedidoRepository;
    }

    // Detalles de un pedido puntual
    @ResponseBody
    @GetMapping("/pedido/{pedidoId}")
    public java.util.List<DetallePedido> getByPedido(@PathVariable Long pedidoId) {
        try {
            return detallePedidoRepository.findByPedidoId(pedidoId);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
