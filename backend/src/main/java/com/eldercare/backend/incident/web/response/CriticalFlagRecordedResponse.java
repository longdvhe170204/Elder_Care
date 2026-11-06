package com.eldercare.backend.incident.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Phản hồi 201 của {@code POST /residents/{residentId}/critical-flags}: mã dấu và mã cảnh báo nguy kịch. */
public record CriticalFlagRecordedResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long flagId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long alertId) {}
