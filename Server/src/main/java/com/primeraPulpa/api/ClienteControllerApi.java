package com.primeraPulpa.api;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.Services.ClienteService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/clientes")
public class ClienteControllerApi extends BaseControllerApi<Cliente, Long> {

    public ClienteControllerApi(ClienteService service) {
        super(service);
    }
}
