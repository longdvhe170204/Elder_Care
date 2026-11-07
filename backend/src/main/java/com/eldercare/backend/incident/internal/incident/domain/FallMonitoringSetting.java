package com.eldercare.backend.incident.internal.incident.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Tham số CFG-M05-06 "Theo dõi sau ngã": khoảng đo và thời gian theo dõi (FR-051). */
public record FallMonitoringSetting(Amount interval, Amount duration) {

    /** {@code unit} là tên hằng {@link ChronoUnit} như tham số lưu ({@code HOURS}); Jackson 3 đọc enum này theo tên hiển thị. */
    public record Amount(long value, String unit) {
        public Duration toDuration() {
            return Duration.of(value, ChronoUnit.valueOf(unit));
        }
    }

    /**
     * Mốc theo dõi (CHK013, research S-14): có thời điểm xảy ra thì lấy thời điểm đó nhưng không sớm hơn thời điểm
     * ghi nhận trừ một khoảng đo; không có thì lấy thời điểm ghi nhận.
     */
    public Instant anchor(Instant occurredAt, Instant recordedAt) {
        if (occurredAt == null) return recordedAt;
        var earliest = recordedAt.minus(interval.toDuration());
        return occurredAt.isBefore(earliest) ? earliest : occurredAt;
    }
}
