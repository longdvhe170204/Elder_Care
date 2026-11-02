package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.common.Reason;
import jakarta.validation.constraints.NotNull;

/** Đánh dấu hoặc gỡ dấu Ưu tiên, bắt buộc lý do (FR-009, Q-394). */
public record MarkPriorityBody(
        @NotNull Boolean priority, @NotNull Reason reason) {}
