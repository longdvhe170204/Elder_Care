package com.eldercare.backend.admission.internal.waitinglist.domain;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Hồ sơ chờ (data-model mục 4; dòng {@code WAITING_ENTRY} của {@code admission_record}). {@code applicationDate}, {@code
 * residenceType} lấy từ lượt đăng ký (FR-007). {@code priority} là dấu Ưu tiên có lý do (FR-009, Q-394). {@code lastConfirmedAt}
 * là mốc tính CFG-M02-03.
 */
public record WaitingListEntry(
        long id,
        long applicationId,
        long residentId,
        CareLevel careLevel,
        boolean careLevelNotAssessed,
        boolean priority,
        String priorityReason,
        Long heldBedId,
        Instant lastConfirmedAt,
        WaitingListStatus status,
        int version,
        long createdBy,
        Instant createdAt,
        LocalDate applicationDate,
        ResidenceType residenceType) {}
