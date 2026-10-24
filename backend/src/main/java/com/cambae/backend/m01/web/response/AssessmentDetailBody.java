package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết lần đánh giá kèm dữ liệu tham khảo (contracts/api-001.yaml, schema {@code AssessmentDetail} = {@code
 * Assessment} + {@code reference}; FR-024).
 */
@Schema(name = "AssessmentDetail")
public record AssessmentDetailBody(
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
        boolean corrected,
        Reference reference) {

    /** Dữ liệu để bác sĩ tham khảo; chỉ số gần nhất lấy từ 007 nên rỗng tới khi 007 cài {@code VitalSignApi}. */
    @Schema(name = "AssessmentReference")
    public record Reference(
            InitialHealthRecordBody initialHealthRecord,
            List<HealthItemBody> activeHealthItems,
            List<Object> latestVitalSigns) {}

    public static AssessmentDetailBody of(AssessmentBody a, Reference reference) {
        return new AssessmentDetailBody(
                a.id(),
                a.type(),
                a.basis(),
                a.createdBy(),
                a.comment(),
                a.careNote(),
                a.careLevel(),
                a.careNeeds(),
                a.proposedResidenceType(),
                a.status(),
                a.confirmedBy(),
                a.confirmedAt(),
                a.corrected(),
                reference);
    }
}
