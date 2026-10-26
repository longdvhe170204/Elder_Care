package com.cambae.backend.m02.internal.ending.domain;

import com.cambae.backend.m02.api.dto.EndCase;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Hồ sơ kết thúc lưu trú (data-model mục 9; FR-062). {@code actualEndedAt} là thời điểm hiệu lực của lệnh Kết thúc lưu trú,
 * có khi Hoàn tất. {@code overdueSince} là dấu "quá ngày dự kiến" (FR-066a). Hồ sơ đã Hoàn tất hoặc Đã hủy không sửa nữa.
 */
public record ResidencyEnding(
        long id,
        long residentId,
        EndCase endCase,
        boolean medicalReason,
        String destination,
        LocalDate expectedEndDate,
        Instant actualEndedAt,
        String requestedBy,
        String reason,
        LocalDate overdueSince,
        Long finalPeriodRef,
        EndingStatus status,
        String cancelReason,
        long createdBy,
        Instant createdAt) {

    public boolean preparing() {
        return status == EndingStatus.PREPARING;
    }
}
