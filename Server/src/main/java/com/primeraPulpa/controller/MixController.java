package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.Services.MixService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping(path = "/mixes")
public class MixController extends BaseController<Mix, Long> {

    public MixController(MixService service) {
        super(service, Mix.class, "/mixes", "mix");
    }

    @Override
    protected String vistaFormulario() {
        return "mix/form";
    }

    @Override
    protected String vistaListado() {
        return "mix/list";
    }

    @Override
    protected String vistaDetalle() {
        return "mix/detail";
    }

    protected List<Mix> filtrarPorNombre(List<Mix> lista, Map<String, String> params) {
        if (params.containsKey("nombre") && params.get("nombre") != null && !params.get("nombre").trim().isEmpty()) {
            String termino = params.get("nombre").toLowerCase().trim();
            return lista.stream()
                    .filter(m -> m.getNombre() != null && m.getNombre().toLowerCase().contains(termino))
                    .collect(Collectors.toList());
        }
        return lista;
    }
}
