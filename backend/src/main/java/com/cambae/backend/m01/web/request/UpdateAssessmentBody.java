package com.cambae.backend.m01.web.request;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.DraftContent;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Thân sửa nháp đánh giá (FR-025, FR-029): thay toàn bộ các trường; mọi trường tùy chọn tới lúc xác nhận. */
public record UpdateAssessmentBody(
        String basis,
        String comment,
        @Size(max = 1000) String careNote,
        CareLevel careLevel,
        Set<Long> careNeedIds,
        ResidenceType proposedResidenceType) {

    public DraftContent content() {
        return new DraftContent(basis, comment, careNote, careLevel, careNeedIds, proposedResidenceType);
    }
}
