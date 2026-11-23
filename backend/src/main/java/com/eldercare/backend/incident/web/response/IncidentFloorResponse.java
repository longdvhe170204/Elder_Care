package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.incident.domain.IncidentFloor;
import io.swagger.v3.oas.annotations.media.Schema;

/** Schema {@code IncidentFloor} của api-007. */
@Schema(name = "IncidentFloor")
public record IncidentFloorResponse(long id, String name, String buildingName, boolean current) {
    public static IncidentFloorResponse of(IncidentFloor floor) {
        return new IncidentFloorResponse(floor.id(), floor.name(), floor.buildingName(), floor.current());
    }
}
