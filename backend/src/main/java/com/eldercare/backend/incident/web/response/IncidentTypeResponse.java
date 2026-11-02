package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;

/** Loại sự cố trả về qua HTTP. */
public record IncidentTypeResponse(
        long id,
        String code,
        String name,
        IncidentLevel defaultLevel,
        boolean nonHealth,
        boolean allowsNoResident,
        boolean active) {
    public static IncidentTypeResponse from(IncidentTypeView view) {
        return new IncidentTypeResponse(
                view.id(),
                view.code(),
                view.name(),
                view.defaultLevel(),
                view.nonHealth(),
                view.allowsNoResident(),
                view.active());
    }
}
