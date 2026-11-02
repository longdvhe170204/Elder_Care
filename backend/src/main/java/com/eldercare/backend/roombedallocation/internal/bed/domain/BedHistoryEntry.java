package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import java.time.Instant;

/** Ảnh đọc lịch sử trạng thái gốc, T041; đính chính nghiệp vụ triển khai tại T047. */
public record BedHistoryEntry(
        long id,
        BedStatus from,
        BedStatus to,
        String event,
        Instant occurredAt,
        Instant plannedAt,
        boolean executedLate,
        Long actorId,
        String reason,
        String basisType,
        long basisId,
        Boolean disinfected,
        Boolean ppeUsed,
        String note) {}
