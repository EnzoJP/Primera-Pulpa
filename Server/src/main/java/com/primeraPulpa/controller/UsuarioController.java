package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.Services.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/usuarios")
public class UsuarioController extends BaseController<Usuario, Long> {

    public UsuarioController(UsuarioService service) {
        super(service, Usuario.class, "/api/v1/usuarios", "usuario");
    }
}

