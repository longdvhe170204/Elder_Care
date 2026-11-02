package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.admission.internal.application.domain.OpinionChoice;
import com.eldercare.backend.admission.web.response.OpinionSourceBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thân ghi ý kiến về việc vào ở (contracts/api-004.yaml, POST {@code …/admission-opinion}; FR-001, FR-004a). */
public record RecordAdmissionOpinionBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OpinionChoice opinion,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OpinionSourceBody source,

        @Size(max = 2000) String note) {}
