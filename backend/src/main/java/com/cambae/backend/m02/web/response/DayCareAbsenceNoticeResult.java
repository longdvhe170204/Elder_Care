package com.cambae.backend.m02.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Kết quả ghi báo vắng bán trú (contracts/api-004.yaml, POST {@code /day-care-absence-notices}; FR-060). */
public record DayCareAbsenceNoticeResult(
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean withinNoticePeriod) {}
