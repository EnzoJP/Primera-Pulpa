package com.primeraPulpa.api;

import com.primeraPulpa.entities.Rol;
import com.primeraPulpa.Services.RolService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/roles")
public class RolControllerApi extends BaseControllerApi<Rol, Long> {

    public RolControllerApi(RolService service) {
        super(service);
    }
}
