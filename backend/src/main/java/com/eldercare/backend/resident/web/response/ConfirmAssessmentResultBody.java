package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.ConfirmResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Kết quả xác nhận kèm tác động (contracts/api-001.yaml, schema {@code ConfirmAssessmentResult}). */
@Schema(name = "ConfirmAssessmentResult")
public record ConfirmAssessmentResultBody(AssessmentBody assessment, Effects effects) {

    @Schema(name = "ConfirmAssessmentEffects")
    public record Effects(
            boolean currentCareLevelChanged,
            List<Long> supersededRequestIds,
            boolean administratorNotified,
            boolean carePlanReviewNotified) {}

    public static ConfirmAssessmentResultBody of(ConfirmResult r) {
        return new ConfirmAssessmentResultBody(
                AssessmentBody.of(r.assessment()),
                new Effects(
                        r.effects().careLevelChanged(),
                        r.effects().supersededRequestIds(),
                        r.effects().administratorNotified(),
                        r.effects().carePlanReviewNotified()));
    }
}
