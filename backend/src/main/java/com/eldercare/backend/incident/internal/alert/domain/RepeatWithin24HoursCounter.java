package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/**
 * Đếm phát sinh mức Trung bình của một khóa loại trong khoảng trượt (t − khoảng, t] (FR-038a, research S-08
 * {@code DemLap24h}); khoảng lấy từ CFG-M05-02, mặc định 24 giờ.
 */
public final class RepeatWithin24HoursCounter {
    private RepeatWithin24HoursCounter() {}

    public static int count(Collection<Instant> mediumOccurrences, Instant at, Duration window) {
        Instant from = at.minus(window);
        return (int) mediumOccurrences.stream()
                .filter(time -> time.isAfter(from) && !time.isAfter(at))
                .count();
    }

    public static boolean reached(int count, int threshold) {
        return count >= threshold;
    }
}
