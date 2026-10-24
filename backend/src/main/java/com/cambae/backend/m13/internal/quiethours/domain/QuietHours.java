package com.cambae.backend.m13.internal.quiethours.domain;

import com.cambae.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Giờ yên tĩnh CFG-M13-02 ({@code GioYenTinh}; FR-024, research B-06): khoảng giờ theo giờ Việt Nam, có thể qua nửa
 * đêm; mốc bắt đầu thuộc, mốc kết thúc không thuộc. Hai mốc bằng nhau là không có giờ yên tĩnh.
 */
public record QuietHours(LocalTime from, LocalTime to) {

    /** Dạng giá trị bảng của CFG-M13-02: {@code {"from":"21:00","to":"07:00"}}. */
    public record Value(String from, String to) {}

    public static QuietHours of(Value v) {
        return new QuietHours(LocalTime.parse(v.from()), LocalTime.parse(v.to()));
    }

    /** Thời điểm kết thúc giờ yên tĩnh gần nhất nếu {@code at} nằm trong giờ yên tĩnh. */
    public Optional<Instant> endIfWithin(Instant at) {
        if (from.equals(to)) {
            return Optional.empty();
        }
        ZonedDateTime local = at.atZone(BusinessTime.ZONE);
        LocalTime t = local.toLocalTime();
        LocalDate day = local.toLocalDate();
        if (from.isBefore(to)) {
            return !t.isBefore(from) && t.isBefore(to) ? Optional.of(at(day, to)) : Optional.empty();
        }
        if (!t.isBefore(from)) {
            return Optional.of(at(day.plusDays(1), to));
        }
        return t.isBefore(to) ? Optional.of(at(day, to)) : Optional.empty();
    }

    private static Instant at(LocalDate day, LocalTime time) {
        return day.atTime(time).atZone(BusinessTime.ZONE).toInstant();
    }
}
