package com.cambae.backend.m09.internal.domain;

import java.time.Instant;

/** Dữ liệu đọc nội bộ; service dựng view công khai, không để lớp web dùng bảng lưu trữ. */
public final class StaffStatisticsRows {
    private StaffStatisticsRows() {}

    public record Handover(long id, long shiftId, String status, Instant preparedAt, Instant confirmedAt) {}

    public record Assignment(
            long id,
            long staffId,
            Long shiftId,
            String targetType,
            long targetId,
            Instant from,
            Instant to,
            String role) {}
}
