package com.primeraPulpa.api;

import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.repositories.DetallePedidoRepository;
import com.primeraPulpa.Services.DetallePedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/detalles-pedido")
public class DetallePedidoControllerApi extends BaseControllerApi<DetallePedido, Long> {

    private final DetallePedidoRepository detallePedidoRepository;

    public DetallePedidoControllerApi(DetallePedidoService service, DetallePedidoRepository detallePedidoRepository) {
        super(service);
        this.detallePedidoRepository = detallePedidoRepository;
    }

    // Detalles de un pedido puntual
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<?> getByPedido(@PathVariable Long pedidoId) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(detallePedidoRepository.findByPedidoId(pedidoId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error de Sistema"));
        }
    }
}
