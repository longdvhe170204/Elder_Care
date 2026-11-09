package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh Chuyển sự cố, schema {@code IncidentFromAlert} của api-007. */
public record ConvertToIncidentRequest(
        Long incidentTypeId,
        String description,
        String initialResponse,

        @Schema(description = "Bắt buộc khi cảnh báo chưa gắn người cao tuổi (FR-047i)")
        Long residentId) {}
