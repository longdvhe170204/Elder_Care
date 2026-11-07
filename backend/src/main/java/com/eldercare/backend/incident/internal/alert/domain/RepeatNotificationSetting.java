package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/** Tham số CFG-M05-02 "báo lặp lại": số lần trong khoảng trượt (mặc định 3 lần / 24 giờ; FR-038a, BR-M05-03). */
public record RepeatNotificationSetting(int count, Window window) {

    /** Đơn vị lưu như mọi thời lượng của tham số ({@code "HOURS"}); {@code ChronoUnit} của Jackson đọc theo tên hiển thị. */
    public record Window(long value, String unit) {
        public Duration toDuration() {
            return Duration.of(value, ChronoUnit.valueOf(unit));
        }
    }
}
