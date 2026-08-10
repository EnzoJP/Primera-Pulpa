package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.Services.FormulaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/formulas")
public class FormulaController extends BaseController<Formula, Long> {

    private final FormulaRepository formulaRepository;

    public FormulaController(FormulaService service, FormulaRepository formulaRepository) {
        super(service, Formula.class, "/api/v1/formulas", "formula");
        this.formulaRepository = formulaRepository;
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
}
