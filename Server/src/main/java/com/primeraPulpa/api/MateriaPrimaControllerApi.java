package com.primeraPulpa.api;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.Services.MateriaPrimaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/materias-primas")
public class MateriaPrimaControllerApi extends BaseControllerApi<MateriaPrima, Long> {

    private final MateriaPrimaRepository materiaPrimaRepository;

    public MateriaPrimaControllerApi(MateriaPrimaService service, MateriaPrimaRepository materiaPrimaRepository) {
        super(service);
        this.materiaPrimaRepository = materiaPrimaRepository;
    }

    // HU-06: materias primas con stock por debajo del mínimo, para la alerta del dashboard
    @GetMapping("/stock-bajo")
    public ResponseEntity<?> getStockBajo() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(materiaPrimaRepository.findConStockBajo());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error de Sistema"));
        }
    }
}
