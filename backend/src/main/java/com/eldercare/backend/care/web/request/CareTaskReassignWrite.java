package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** T056: thân lệnh phân lại (FR-031); bản dịch api-005 chủ dự án duyệt 2026-11-05. */
public record CareTaskReassignWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long recipientId,

        @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason) {}
