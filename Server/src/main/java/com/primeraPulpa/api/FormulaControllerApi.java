package com.primeraPulpa.api;

import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.repositories.FormulaRepository;
import com.primeraPulpa.Services.FormulaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/formulas")
public class FormulaControllerApi extends BaseControllerApi<Formula, Long> {

    private final FormulaRepository formulaRepository;

    public FormulaControllerApi(FormulaService service, FormulaRepository formulaRepository) {
        super(service);
        this.formulaRepository = formulaRepository;
    }

    // HU-07 / HU-11: todos los componentes (fórmula completa) de un mix
    @GetMapping("/mix/{mixId}")
    public ResponseEntity<?> getByMix(@PathVariable Long mixId) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(formulaRepository.findByMixId(mixId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error de Sistema"));
        }
    }
}
