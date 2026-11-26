package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /emergency-assist/{id}/messages}: tin người bấm nhắn thêm, tối đa 200 ký tự (đề xuất 2026-11-23). */
@Schema(name = "AssistMessage")
public record AssistMessageRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        String text) {}
