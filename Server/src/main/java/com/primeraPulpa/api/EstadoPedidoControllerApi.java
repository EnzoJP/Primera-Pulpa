package com.primeraPulpa.api;

import com.primeraPulpa.entities.EstadoPedido;
import com.primeraPulpa.Services.EstadoPedidoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/estado-pedidos")
public class EstadoPedidoControllerApi extends BaseControllerApi<EstadoPedido, Long> {

    public EstadoPedidoControllerApi(EstadoPedidoService service) {
        super(service);
    }
}
