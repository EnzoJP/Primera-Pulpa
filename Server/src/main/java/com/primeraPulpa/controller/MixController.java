package com.primeraPulpa.controller;

import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.Services.MixService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin("*")
@RequestMapping(path = "api/v1/mixes")
public class MixController extends BaseController<Mix, Long> {

    public MixController(MixService service) {
        super(service, Mix.class, "/api/v1/mixes", "mix");
    }
}
