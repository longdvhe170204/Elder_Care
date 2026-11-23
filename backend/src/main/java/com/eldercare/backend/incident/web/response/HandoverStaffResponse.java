package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.emergency.service.HandoverStaffService;
import io.swagger.v3.oas.annotations.media.Schema;

/** Một nhân viên có thể chọn bàn giao ở lệnh Chuyển viện, schema {@code HandoverStaff} của api-007 (FR-049). */
@Schema(name = "HandoverStaff")
public record HandoverStaffResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        String position) {

    public static HandoverStaffResponse of(HandoverStaffService.Candidate candidate) {
        return new HandoverStaffResponse(candidate.id(), candidate.fullName(), candidate.position());
    }
}
