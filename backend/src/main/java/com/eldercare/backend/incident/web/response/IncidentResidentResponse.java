package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.incident.domain.IncidentResident;
import io.swagger.v3.oas.annotations.media.Schema;

/** Schema {@code IncidentResident} của api-007 (đề xuất 2026-11-23): người cao tuổi tìm được khi báo khẩn. */
@Schema(name = "IncidentResident")
public record IncidentResidentResponse(
        Long id,
        String fullName,
        String profileCode,
        String status,
        @Schema(types = {"string", "null"}) String roomName,
        @Schema(types = {"integer", "null"}) Long floorAreaId,
        @Schema(types = {"string", "null"}) String floorName) {
    public static IncidentResidentResponse of(IncidentResident resident) {
        return new IncidentResidentResponse(
                resident.id(),
                resident.fullName(),
                resident.profileCode(),
                resident.status(),
                resident.roomName(),
                resident.floorAreaId(),
                resident.floorName());
    }
}
