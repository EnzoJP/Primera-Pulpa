package com.primeraPulpa.api;

import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.repositories.PedidoRepository;
import com.primeraPulpa.Services.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/pedidos")
public class PedidoControllerApi extends BaseControllerApi<Pedido, Long> {

    private final PedidoRepository pedidoRepository;

    public PedidoControllerApi(PedidoService service, PedidoRepository pedidoRepository) {
        super(service);
        this.pedidoRepository = pedidoRepository;
    }

    // Dashboard de cliente / HU-13
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<?> getByCliente(@PathVariable Long clienteId) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(pedidoRepository.findByClienteId(clienteId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error de Sistema"));
        }
    }
}
