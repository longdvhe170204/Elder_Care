package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.internal.examination.service.ExaminationService.ExaminationInput;
import com.eldercare.backend.health.internal.examination.service.ExaminationService.ExaminationView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Schema {@code ExaminationRecord} của api-007: thân lệnh ghi và bản ghi trả về (FR-072). */
@Schema(name = "ExaminationRecord")
public record ExaminationRecordBody(
        Long id,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"EXAMINATION", "ASSESSMENT", "DIAGNOSIS", "ORDER", "TREATMENT", "RESULT"})
        String type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean atFacility,
        String externalFacilityName,
        Long performerId,
        String externalPerformerName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant performedAt,
        List<Long> fileIds) {

    public ExaminationInput toInput() {
        return new ExaminationInput(
                type,
                content,
                Boolean.TRUE.equals(atFacility),
                externalFacilityName,
                performerId,
                externalPerformerName,
                performedAt,
                fileIds);
    }

    public static ExaminationRecordBody from(ExaminationView view) {
        return new ExaminationRecordBody(
                view.id(),
                view.type(),
                view.content(),
                view.atFacility(),
                view.externalFacilityName(),
                view.performerId(),
                view.externalPerformerName(),
                view.performedAt(),
                view.fileIds());
    }
}
