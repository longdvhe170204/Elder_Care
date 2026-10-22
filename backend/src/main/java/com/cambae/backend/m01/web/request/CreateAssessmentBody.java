package com.cambae.backend.m01.web.request;

import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh tạo lần đánh giá ở Nháp (contracts/api-001.yaml, POST {@code /residents/{id}/assessments}). */
public record CreateAssessmentBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AssessmentType type,

        String basis) {}
