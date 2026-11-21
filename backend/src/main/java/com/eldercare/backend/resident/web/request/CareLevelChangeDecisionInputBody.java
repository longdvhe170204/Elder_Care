package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision.Decision;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeDecisionService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Ghi quyết định khi gia đình không đồng ý đổi mức (api-001 {@code CareLevelChangeDecisionInput}; FR-036b). */
@Schema(name = "CareLevelChangeDecisionInput")
public record CareLevelChangeDecisionInputBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long assessmentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Decision decision,

        @Schema(description = "Bắt buộc khi KEEP_CURRENT_LEVEL")
        Long riskAgreementId,

        @Schema(description = "Bắt buộc khi KEEP_CURRENT_LEVEL, sau hôm nay")
        LocalDate reviewDate,

        @Schema(maxLength = 1000) String note) {

    public CareLevelChangeDecisionService.Input toInput() {
        return new CareLevelChangeDecisionService.Input(assessmentId, decision, riskAgreementId, reviewDate, note);
    }
}
