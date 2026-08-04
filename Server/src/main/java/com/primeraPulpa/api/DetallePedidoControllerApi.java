package com.primeraPulpa.api;

import com.primeraPulpa.api.BaseControllerApi;
import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.Services.DetallePedidoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/detalle-pedidos")
public class DetallePedidoControllerApi extends BaseControllerApi<DetallePedido, Long> {

    public DetallePedidoControllerApi(DetallePedidoService service) {
        super(service);
    }
}
