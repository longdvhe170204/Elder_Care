package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.admission.internal.change.domain.ResidentOpinion;
import com.eldercare.backend.admission.internal.change.service.ResidencyChangeService;
import io.swagger.v3.oas.annotations.media.Schema;

/** Ghi ý kiến người cao tuổi (api-004 {@code ResidentOpinionInput}; FR-043h). */
@Schema(name = "ResidentOpinionInput")
public record ResidentOpinionInputBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ResidentOpinion.Opinion opinion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ResidentOpinion.Source source,
        @Schema(maxLength = ResidentOpinion.NOTE_MAX) String note) {

    public ResidencyChangeService.OpinionInput toInput() {
        return new ResidencyChangeService.OpinionInput(opinion, source, note);
    }
}
