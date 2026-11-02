package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Lần đánh giá (contracts/api-001.yaml, schema {@code Assessment}); không có điểm hay mức đề xuất (FR-024). */
@Schema(name = "Assessment")
public record AssessmentBody(
        long id,
        AssessmentType type,
        String basis,
        String createdBy,
        String comment,
        String careNote,
        CareLevel careLevel,
        List<CatalogItemBody> careNeeds,
        ResidenceType proposedResidenceType,
        AssessmentStatus status,
        String confirmedBy,
        Instant confirmedAt,
        boolean corrected) {

    public static AssessmentBody of(AssessmentView v) {
        return new AssessmentBody(
                v.id(),
                v.type(),
                v.basis(),
                v.createdBy(),
                v.comment(),
                v.careNote(),
                v.careLevel(),
                v.careNeeds().stream()
                        .map(n -> new CatalogItemBody(n.id(), n.name(), null, null, null))
                        .toList(),
                v.proposedResidenceType(),
                v.status(),
                v.confirmedBy(),
                v.confirmedAt(),
                v.corrected());
    }
}
