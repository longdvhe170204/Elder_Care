package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh giao người phụ trách cảnh báo: mã nhân viên được giao và ghi chú bắt buộc (bảng FR-029). */
public record AssignAlertRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long staffId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String note) {}
