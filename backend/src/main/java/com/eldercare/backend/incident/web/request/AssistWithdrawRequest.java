package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /emergency-assist/{id}/withdraw} (không bắt buộc): lý do rút, tối đa 200 ký tự (đề xuất 2026-11-23). */
@Schema(name = "AssistWithdraw")
public record AssistWithdrawRequest(
        @Schema(
                types = {"string", "null"},
                maxLength = 200)
        String reason) {}
