package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh giao người xử lý sự cố: người được giao (mã tài khoản) và ghi chú bắt buộc (FR-045a). */
public record AssignIncidentHandlerRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long assigneeId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String note) {}
