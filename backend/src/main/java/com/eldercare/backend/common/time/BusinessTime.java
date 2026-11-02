package com.eldercare.backend.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Quy đổi giữa thời điểm và ngày lịch nghiệp vụ theo Asia/Ho_Chi_Minh (research R-04; FR-008, FR-046, BR-M15-07).
 */
@Component
public class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;

    public BusinessTime(Clock clock) {
        this.clock = clock;
    }

    /** Ngày lịch hôm nay theo giờ Việt Nam. */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZONE);
    }

    /** Thời điểm hiện tại của máy chủ. */
    public Instant now() {
        return clock.instant();
    }

    /** 00:00 giờ Việt Nam của ngày đã cho, mốc bắt đầu hiệu lực (BR-M15-07). */
    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    /** {@code date} nằm trong khoảng gồm cả hai đầu; {@code to} là {@code null} nghĩa là khoảng còn mở (FR-008). */
    public boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && (to == null || !date.isAfter(to));
    }
}
