package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.FamilyDeclaredView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Phần Hành chính thấy của lần đánh giá đầu vào Nháp (Q-412, FR-025a); không có nhận xét, mức. */
@Schema(name = "FamilyDeclaredAssessment")
public record FamilyDeclaredAssessmentBody(
        long assessmentId,
        @Schema(allowableValues = {"DRAFT"}) String status,
        List<FamilyDeclaredEntryBody> entries) {

    public static FamilyDeclaredAssessmentBody of(FamilyDeclaredView v) {
        return new FamilyDeclaredAssessmentBody(
                v.assessmentId(),
                "DRAFT",
                v.entries().stream().map(FamilyDeclaredEntryBody::of).toList());
    }
}
