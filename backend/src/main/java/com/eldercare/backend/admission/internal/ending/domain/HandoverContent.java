package com.eldercare.backend.admission.internal.ending.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/** Nội dung bàn giao người cao tuổi (nhóm 3, loại {@code M02.RESIDENT_HANDOVER} của {@code record_event}; FR-063 (e)). */
public record HandoverContent(
        RecipientType recipientType,
        Long familyMemberId,
        String facilityName,
        Instant handoverAt,
        long handoverStaffId,
        Long pickupRecordId) {

    public static final SharedKind<HandoverContent> KIND =
            SharedKind.of("M02.RESIDENT_HANDOVER", HandoverContent.class);
}
