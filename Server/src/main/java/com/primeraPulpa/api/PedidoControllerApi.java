package com.primeraPulpa.api;

import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.Services.PedidoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/pedidos")
public class PedidoControllerApi extends BaseControllerApi<Pedido, Long> {

    public PedidoControllerApi(PedidoService service) {
        super(service);
    }
}
