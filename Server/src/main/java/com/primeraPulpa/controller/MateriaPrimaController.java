package com.primeraPulpa.controller;

import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.UnidadMedida;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.UnidadMedidaRepository;
import com.primeraPulpa.Services.MateriaPrimaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;

@Controller
@RequestMapping(path = "/materias-primas")
public class MateriaPrimaController extends BaseController<MateriaPrima, Long> {

    private final MateriaPrimaRepository materiaPrimaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public MateriaPrimaController(MateriaPrimaService service, MateriaPrimaRepository materiaPrimaRepository,
                                  UnidadMedidaRepository unidadMedidaRepository) {
        super(service, MateriaPrima.class, "/materias-primas", "materiaPrima");
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    // Convierte el id enviado por el select en la entidad UnidadMedida correspondiente
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(UnidadMedida.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                setValue(unidadMedidaRepository.findById(Long.valueOf(text)).orElse(null));
            }
        });
    }

    // HU-03: carga las unidades de medida disponibles para el formulario
    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("unidades", unidadMedidaRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getEliminado()))
                .toList());
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

    // HU-03: formulario de alta/editado propio de materia prima (no incluye carga de stock)
    @Override
    protected String vistaFormulario() {
        return "materia-prima/form";
    }

    @Override
    protected String vistaListado() {
        return "materia-prima/list";
    }

    @Override
    protected String vistaDetalle() {
        return "materia-prima/detail";
    }
}
