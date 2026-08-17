package com.primeraPulpa.controller;

import com.primeraPulpa.entities.CostoAdicional;
import com.primeraPulpa.Services.CostoAdicionalService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/costos-adicionales")
public class CostoAdicionalController extends BaseController<CostoAdicional, Long> {

    public CostoAdicionalController(CostoAdicionalService service) {
        super(service, CostoAdicional.class, "/costos-adicionales", "costo adicional");
    }

    @Override
    protected String vistaFormulario() {
        return "costo-adicional/form";
    }

    @Override
    protected String vistaListado() {
        return "costo-adicional/list";
    }

    @Override
    protected String vistaDetalle() {
        return "costo-adicional/detail";
    }
}