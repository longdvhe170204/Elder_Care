package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.DraftContent;
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
