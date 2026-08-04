package com.primeraPulpa.api;

import com.primeraPulpa.api.BaseControllerApi;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.Services.MixService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping(path = "api/v1/mixes")
public class MixControllerApi extends BaseControllerApi<Mix, Long> {

    public MixControllerApi(MixService service) {
        super(service);
    }
}
