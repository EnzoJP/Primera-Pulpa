package com.primeraPulpa.api;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.Services.MateriaPrimaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/materias-primas")
public class MateriaPrimaControllerApi extends BaseControllerApi<MateriaPrima, Long> {

    public MateriaPrimaControllerApi(MateriaPrimaService service) {
        super(service);
    }
}

