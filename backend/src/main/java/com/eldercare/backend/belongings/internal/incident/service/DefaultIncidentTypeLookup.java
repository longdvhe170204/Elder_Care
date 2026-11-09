package com.eldercare.backend.belongings.internal.incident.service;

import com.eldercare.backend.incident.api.IncidentTypeApi;
import org.springframework.stereotype.Component;

/** Tra mã loại sự cố đồ gửi qua {@link IncidentTypeApi} của 007 (bản thật, 007 `V007_13`). */
@Component
class DefaultIncidentTypeLookup implements IncidentTypeLookup {

    private final IncidentTypeApi types;

    DefaultIncidentTypeLookup(IncidentTypeApi types) {
        this.types = types;
    }

    @Override
    public long idOf(String typeCode) {
        return types.idOf(typeCode);
    }
}
