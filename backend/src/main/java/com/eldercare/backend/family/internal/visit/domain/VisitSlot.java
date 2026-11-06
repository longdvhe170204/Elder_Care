package com.eldercare.backend.family.internal.visit.domain;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Khung thăm (CFG-M10-04; {@code khung_tham_ngay} theo ngày; R-15: khóa {@code technical_lock} theo {@link #lockKey}).
 * Giờ theo giờ Việt Nam.
 */
public record VisitSlot(LocalTime start, LocalTime end) {

    /** "09:00-11:00". */
    public String label() {
        return start + "-" + end;
    }

    public static VisitSlot parse(String label) {
        String[] parts = label.split("-");
        return new VisitSlot(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
    }

    public Instant startOn(LocalDate date) {
        return ZonedDateTime.of(date, start, BusinessTime.ZONE).toInstant();
    }

    public Instant endOn(LocalDate date) {
        return ZonedDateTime.of(date, end, BusinessTime.ZONE).toInstant();
    }

    /** Khóa tuần tự hóa đăng ký, hủy, Không đến của khung trong ngày (FR-030 (b)). */
    public String lockKey(LocalDate date) {
        return "M10.VISIT_SLOT:" + date + ":" + label();
    }
}
