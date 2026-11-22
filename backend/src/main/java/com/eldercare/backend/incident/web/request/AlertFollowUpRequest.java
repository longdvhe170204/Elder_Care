package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh ghi kết quả theo dõi cảnh báo (FR-087, Q-419). */
public record AlertFollowUpRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String content) {}
