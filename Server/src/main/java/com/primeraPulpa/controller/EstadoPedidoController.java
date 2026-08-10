package com.primeraPulpa.controller;

import com.primeraPulpa.entities.EstadoPedido;
import com.primeraPulpa.Services.EstadoPedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/estado-pedidos")
public class EstadoPedidoController extends BaseController<EstadoPedido, Long> {

    public EstadoPedidoController(EstadoPedidoService service) {
        super(service, EstadoPedido.class, "/api/v1/estado-pedidos", "estadoPedido");
    }
}
