package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.Services.RolService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/roles")
public class RolController extends BaseController<Rol, Long> {

    public RolController(RolService service) {
        super(service, Rol.class, "/roles", "rol");
    }
}
