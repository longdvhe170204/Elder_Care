package com.eldercare.backend.care.internal.wellbeing.domain;

import java.time.LocalDate;

/** FR-054: vắng dù một phần ngày, nghi nhiễm, không ghi nhận hoặc bán trú không đến đều bỏ qua. */
public final class CountingDay {
    private CountingDay() {}

    public static boolean counts(
            LocalDate day,
            LocalDate admittedOn,
            boolean absent,
            boolean suspectedInfection,
            boolean unknownGroupAttendance,
            boolean dayCare,
            boolean arrived) {
        return !day.isBefore(admittedOn)
                && !absent
                && !suspectedInfection
                && !unknownGroupAttendance
                && (!dayCare || arrived);
    }
}
