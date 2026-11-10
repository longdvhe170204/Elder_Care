package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.AssistDetails;
import com.eldercare.backend.incident.internal.alert.domain.AssistEventType;
import com.eldercare.backend.incident.internal.alert.domain.SosPlace;

/** Thân {@code POST /emergency-assist/{id}/details} (FR-047i): mỗi trường đặt một lần. */
public record AssistDetailsRequest(AssistEventType eventType, Long floorAreaId, SosPlace place, Long residentId) {
    public AssistDetails toDetails() {
        return new AssistDetails(eventType, floorAreaId, place, residentId);
    }
}
