package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.AssistDetails;
import com.eldercare.backend.incident.internal.alert.domain.AssistEventType;
import com.eldercare.backend.incident.internal.alert.domain.SosPlace;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /emergency-assist/{id}/details} (FR-047i): mỗi trường đặt một lần. */
public record AssistDetailsRequest(
        AssistEventType eventType,
        @Schema(type = "integer", format = "int64") Long floorAreaId,
        SosPlace place,
        @Schema(type = "integer", format = "int64") Long residentId) {
    public AssistDetails toDetails() {
        return new AssistDetails(eventType, floorAreaId, place, residentId);
    }
}
