package com.primeraPulpa.controller;

import com.primeraPulpa.entities.EstadoPedido;
import com.primeraPulpa.Services.EstadoPedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/estado-pedidos")
public class EstadoPedidoController extends BaseController<EstadoPedido, Long> {

    public EstadoPedidoController(EstadoPedidoService service) {
        super(service, EstadoPedido.class, "/estado-pedidos", "estadoPedido");
    }
}
