package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh tạo cảnh báo thủ công (FR-026 (c)): người cao tuổi, loại sự cố, mức, mô tả bắt buộc. */
public record CreateAlertRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long incidentTypeId,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"LOW", "MEDIUM", "CRITICAL"})
        String level,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 4000)
        String description) {}
