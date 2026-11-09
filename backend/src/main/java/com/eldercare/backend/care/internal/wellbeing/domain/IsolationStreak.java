package com.eldercare.backend.care.internal.wellbeing.domain;

import java.time.LocalDate;

/** FR-054–056: ngày bỏ qua giữ chuỗi; Có mặt luôn đếm lại, kể cả ngày bỏ qua. */
public record IsolationStreak(int days, LocalDate lastDate, boolean alerted) {
    public IsolationStreak advance(LocalDate day, boolean groupPresent, boolean countingDay, int threshold) {
        if (lastDate != null && !day.isAfter(lastDate)) return this;
        int next = groupPresent ? 0 : days + (countingDay ? 1 : 0);
        return new IsolationStreak(next, day, !groupPresent && (alerted || next >= threshold));
    }
}
