package com.eldercare.backend.medication.internal.reconciliation.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.Map;

/** Nội dung dòng {@code simple_record} loại {@code M07.RECONCILIATION_CORRECTION_REQUEST} (data-model 006 mục 0.1). */
public record ReconciliationCorrectionRequestContent(
        Long reconciliationId,
        String correctedContentJson,
        String reason,
        Long createdById,
        Long confirmedById,
        Instant confirmedAt,
        Long correctionId,
        String returnReason,
        Map<String, Object> legacy) {

    public static final SharedKind<ReconciliationCorrectionRequestContent> KIND = SharedKind.of(
                    "M07.RECONCILIATION_CORRECTION_REQUEST", ReconciliationCorrectionRequestContent.class)
            .notDeletable();
}
