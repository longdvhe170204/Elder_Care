package com.cambae.backend.m02.internal.ending.domain;

import java.time.Instant;

/** Một lần bàn giao người cao tuổi (nhóm 3; FR-063 (e)); giá trị hiện hành áp đính chính lấy từ {@code CorrectionApi}. */
public record Handover(
        long id,
        long endingId,
        RecipientType recipientType,
        Long familyMemberId,
        String facilityName,
        Instant handoverAt,
        long handoverStaffId,
        long recordedBy,
        Instant recordedAt) {}
