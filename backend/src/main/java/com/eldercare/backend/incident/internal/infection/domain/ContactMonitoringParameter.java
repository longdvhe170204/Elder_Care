package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Duration;

/** CFG-M05-08 (bảng): chỉ số cần đo, số lần mỗi ngày, thời gian theo dõi người tiếp xúc (BR-M05-11). */
public record ContactMonitoringParameter(String measurement, int timesPerDay, Length duration) {
    public record Length(long value, String unit) {
        public Duration toDuration() {
            return switch (unit) {
                case "HOURS" -> Duration.ofHours(value);
                case "DAYS" -> Duration.ofDays(value);
                default -> throw new IllegalStateException("CFG-M05-08 có đơn vị không hỗ trợ: " + unit);
            };
        }
    }
}
