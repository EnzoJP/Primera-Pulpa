package com.primeraPulpa.api;

import com.primeraPulpa.entities.Formula;
import com.primeraPulpa.Services.FormulaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/formulas")
public class FormulaControllerApi extends BaseControllerApi<Formula, Long> {

    public FormulaControllerApi(FormulaService service) {
        super(service);
    }
}
