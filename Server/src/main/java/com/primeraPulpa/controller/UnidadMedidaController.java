package com.primeraPulpa.controller;

import com.primeraPulpa.entities.UnidadMedida;
import com.primeraPulpa.Services.UnidadMedidaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/unidades-medida")
public class UnidadMedidaController extends BaseController<UnidadMedida, Long> {

    public UnidadMedidaController(UnidadMedidaService service) {
        super(service, UnidadMedida.class, "/unidades-medida", "unidad de medida");
    }

    @Override
    protected String vistaFormulario() {
        return "unidad-medida/form";
    }

    @Override
    protected String vistaListado() {
        return "unidad-medida/list";
    }

    @Override
    protected String vistaDetalle() {
        return "unidad-medida/detail";
    }
}