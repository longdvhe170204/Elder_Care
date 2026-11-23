package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision;
import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision.Decision;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Quyết định khi gia đình không đồng ý đổi mức chăm sóc (api-001 {@code CareLevelChangeDecision}; FR-036b, Q-424). */
@Schema(name = "CareLevelChangeDecision")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CareLevelChangeDecisionBody(
        long id,
        long residentId,
        long assessmentId,
        Decision decision,
        Long riskAgreementId,
        LocalDate reviewDate,
        String note,
        long recordedBy,
        Instant recordedAt,
        Instant reviewNotifiedAt) {

    public static CareLevelChangeDecisionBody of(CareLevelChangeDecision row) {
        return new CareLevelChangeDecisionBody(
                row.id(),
                row.residentId(),
                row.assessmentId(),
                row.decision(),
                row.riskAgreementId(),
                row.reviewDate(),
                row.note(),
                row.recordedBy(),
                row.recordedAt(),
                row.reviewNotifiedAt());
    }
}
