package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.Services.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/clientes")
public class ClienteController extends BaseController<Cliente, Long> {

    public ClienteController(ClienteService service) {
        super(service, Cliente.class, "/api/v1/clientes", "cliente");
    }
}
