package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.Services.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/clientes")
public class ClienteController extends BaseController<Cliente, Long> {

    public ClienteController(ClienteService service) {
        super(service, Cliente.class, "/clientes", "cliente");
    }
}
