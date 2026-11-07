package com.eldercare.backend.health.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lý do của lệnh M06 cần lý do (ngừng lịch đo, hủy hoặc ngừng ngưỡng; api-007 requestBody {@code Reason}). */
public record HealthReasonRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
