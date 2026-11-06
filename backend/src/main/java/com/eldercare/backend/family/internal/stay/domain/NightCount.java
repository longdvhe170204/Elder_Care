package com.eldercare.backend.family.internal.stay.domain;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Số đêm tính phí của lượt ở lại ({@code SoDem}; FR-043, BR-M10-04): số mốc 00:00 (giờ của viện) nằm sau lúc bắt đầu và
 * không sau lúc kết thúc. Mỗi đêm mang ngày bắt đầu của đêm (ngày trước mốc), là ngày phát sinh gửi 010.
 */
public final class NightCount {

    private NightCount() {}

    public static int count(Instant start, Instant end) {
        return nights(start, end).size();
    }

    public static List<LocalDate> nights(Instant start, Instant end) {
        List<LocalDate> nights = new ArrayList<>();
        LocalDate day = start.atZone(BusinessTime.ZONE).toLocalDate();
        for (LocalDate next = day.plusDays(1); ; next = next.plusDays(1)) {
            Instant midnight = next.atStartOfDay(BusinessTime.ZONE).toInstant();
            if (midnight.isAfter(end)) {
                return nights;
            }
            if (midnight.isAfter(start)) {
                nights.add(next.minusDays(1));
            }
        }
    }
}
