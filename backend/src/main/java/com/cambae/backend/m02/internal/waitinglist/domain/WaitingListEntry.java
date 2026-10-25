package com.cambae.backend.m02.internal.waitinglist.domain;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Hồ sơ chờ (data-model mục 4, bảng {@code waiting_list_entry}). {@code applicationDate}, {@code residenceType} lấy từ lượt
 * đăng ký (FR-007: ngày đăng ký, loại lưu trú theo lượt). {@code lastConfirmedAt} là mốc tính CFG-M02-03.
 */
public record WaitingListEntry(
        long id,
        long applicationId,
        long residentId,
        CareLevel careLevel,
        boolean careLevelNotAssessed,
        ScoreBreakdown breakdown,
        Long heldBedId,
        Instant lastConfirmedAt,
        WaitingListStatus status,
        int version,
        long createdBy,
        Instant createdAt,
        LocalDate applicationDate,
        ResidenceType residenceType) {

    public int score() {
        return breakdown.total();
    }
}
