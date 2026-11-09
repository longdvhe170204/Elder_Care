package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lý do của lệnh sự cố cần lý do (mở lại xử lý; api-007 requestBody {@code Reason}). */
public record IncidentReasonRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
