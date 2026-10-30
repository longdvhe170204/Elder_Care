package com.eldercare.backend.staffing.duty.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Department")
public record DepartmentBody(
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long leadStaffId,
        boolean active) {
    public static DepartmentBody of(com.eldercare.backend.staffing.duty.internal.domain.Department d) {
        return new DepartmentBody(d.id(), d.name(), d.leadStaffId(), d.active());
    }
}
