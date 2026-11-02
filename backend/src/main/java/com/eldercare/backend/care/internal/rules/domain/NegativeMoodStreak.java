package com.eldercare.backend.care.internal.rules.domain;

import java.time.LocalDate;
import java.util.Set;

/** FR042: ngày thiếu ghi nhận làm đứt chuỗi; nhiều bản ghi trong ngày chỉ tính một lần. */
public final class NegativeMoodStreak {
    private NegativeMoodStreak() {}

    public static int count(Set<LocalDate> negativeDays, LocalDate end) {
        int count = 0;
        for (var day = end; negativeDays.contains(day); day = day.minusDays(1)) count++;
        return count;
    }
}
