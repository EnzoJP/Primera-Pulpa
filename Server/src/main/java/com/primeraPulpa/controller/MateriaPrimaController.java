package com.primeraPulpa.controller;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.Services.MateriaPrimaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/materias-primas")
public class MateriaPrimaController extends BaseController<MateriaPrima, Long> {

    private final MateriaPrimaRepository materiaPrimaRepository;

    public MateriaPrimaController(MateriaPrimaService service, MateriaPrimaRepository materiaPrimaRepository) {
        super(service, MateriaPrima.class, "/api/v1/materias-primas", "materiaPrima");
        this.materiaPrimaRepository = materiaPrimaRepository;
    }

    // HU-06: materias primas con stock por debajo del mínimo, para la alerta del dashboard
    @ResponseBody
    @GetMapping("/stock-bajo")
    public java.util.List<MateriaPrima> getStockBajo() {
        try {
            return materiaPrimaRepository.findConStockBajo();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
