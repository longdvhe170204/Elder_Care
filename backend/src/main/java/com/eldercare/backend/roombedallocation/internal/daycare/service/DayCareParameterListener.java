package com.eldercare.backend.roombedallocation.internal.daycare.service;

import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.events.ParameterChanged;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** FR-040: sau khi CFG được commit, đếm lại theo cấu hình hiện hành. */
@Component
public class DayCareParameterListener {
    private final DayCareRecounting recount;

    public DayCareParameterListener(DayCareRecounting recount) {
        this.recount = recount;
    }

    @ApplicationModuleListener
    public void changed(ParameterChanged event) {
        if (event.code() == ParameterCode.CFG_M03_01) recount.recount();
    }
}
