package com.eldercare.backend.medication.internal.handover.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.Map;

/** Nội dung dòng {@code simple_record} loại {@code M07.ABSENCE_INTERVAL} (data-model 006 mục 0.1). */
public record AbsenceIntervalContent(
        AbsenceIntervalType absenceType,
        Instant fromTime,
        Instant expectedToTime,
        Instant actualToTime,
        Long absenceSessionId,
        Long tripId,
        Long noMedicationOrderId,
        Map<String, Object> legacy) {

    public static final SharedKind<AbsenceIntervalContent> KIND =
            SharedKind.of("M07.ABSENCE_INTERVAL", AbsenceIntervalContent.class).notDeletable();
}
