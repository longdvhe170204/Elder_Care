package com.eldercare.backend.medication.internal.dose.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.Map;

/** Nội dung dòng {@code simple_record} loại {@code M07.DOSE_REVIEW} (data-model 006 mục 0.1). */
public record DoseReviewContent(
        Long doseResultId,
        Long prnUsageId,
        Long floorAreaId,
        String reviewerCandidate,
        Long reviewerId,
        Instant reviewedAt,
        String note,
        Instant updatedAt,
        Map<String, Object> legacy) {

    public static final SharedKind<DoseReviewContent> KIND =
            SharedKind.of("M07.DOSE_REVIEW", DoseReviewContent.class).notDeletable();
}
