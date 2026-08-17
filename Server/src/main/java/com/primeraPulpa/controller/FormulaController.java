package com.primeraPulpa.controller;

import com.primeraPulpa.entities.DetalleFormula;
import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.MixRepository;
import com.primeraPulpa.Services.FormulaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(path = "/formulas")
public class FormulaController extends BaseController<Formula, Long> {

    private final FormulaRepository formulaRepository;
    private final MixRepository mixRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;

    public FormulaController(FormulaService service, FormulaRepository formulaRepository,
                             MixRepository mixRepository, MateriaPrimaRepository materiaPrimaRepository) {
        super(service, Formula.class, "/formulas", "fórmula");
        this.formulaRepository = formulaRepository;
        this.mixRepository = mixRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
    }

    // Convierte el id enviado por el formulario en las entidades Mix y MateriaPrima
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Mix.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                setValue(mixRepository.findById(Long.valueOf(text)).orElse(null));
            }
        });
        binder.registerCustomEditor(MateriaPrima.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                setValue(materiaPrimaRepository.findById(Long.valueOf(text)).orElse(null));
            }
        });
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("mixes", mixRepository.findAll().stream()
                .filter(m -> !Boolean.TRUE.equals(m.getEliminado()))
                .toList());
        normalizarDetalles(model);
    }

    // Deja una fila por materia prima activa en la DB (con sus gramos si ya estaban cargados)
    private void normalizarDetalles(Model model) {
        Object item = model.asMap().get("item");
        if (!(item instanceof Formula formula)) {
            return;
        }

        List<MateriaPrima> activas = materiaPrimaRepository.findAll().stream()
                .filter(mp -> !Boolean.TRUE.equals(mp.getEliminado()))
                .toList();

        Map<Long, DetalleFormula> porMp = new HashMap<>();
        if (formula.getDetalles() != null) {
            formula.getDetalles().forEach(d -> {
                if (d.getMateriaPrima() != null) {
                    porMp.put(d.getMateriaPrima().getId(), d);
                }
            });
        }

        List<DetalleFormula> completos = new ArrayList<>();
        for (MateriaPrima mp : activas) {
            DetalleFormula d = porMp.get(mp.getId());
            if (d == null) {
                d = new DetalleFormula();
                d.setMateriaPrima(mp);
                d.setGramos(0);
            }
            completos.add(d);
        }
        formula.setDetalles(completos);
    }

    // HU-07 / HU-11: todos los componentes (fórmula completa) de un mix
    @ResponseBody
    @GetMapping("/mix/{mixId}")
    public java.util.List<Formula> getByMix(@PathVariable Long mixId) {
        try {
            return formulaRepository.findByMixId(mixId);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @Override
    protected String vistaFormulario() {
        return "formula/form";
    }

    @Override
    protected String vistaListado() {
        return "formula/list";
    }

    @Override
    protected String vistaDetalle() {
        return "formula/detail";
    }
}
