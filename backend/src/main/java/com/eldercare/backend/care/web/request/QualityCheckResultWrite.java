package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 014 T025: schema {@code QualityCheckResult} của api-014 (FR-061, FR-062, FR-064). */
public record QualityCheckResultWrite(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PASS", "FAIL", "NOT_CHECKABLE"})
        String result,

        String note,
        List<Long> photoFileIds,
        @Schema(description = "Bắt buộc khi FAIL") ReworkTask reworkTask) {
    public record ReworkTask(Instant plannedAt, Long performerId, Boolean sharedOnFloor) {}
}
