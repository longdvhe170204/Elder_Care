package com.eldercare.backend.health.internal.schedule.domain;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chuỗi mốc đo của một lịch trong khoảng {@code [from, to)} (research S-05 {@code ChuoiMocDo}), giờ theo múi giờ của
 * viện. Chỉ để kiểm căn cứ {@code MEASUREMENT} và hiển thị; không lưu mốc đo, không sinh công việc (FR-004, Q-306).
 */
public final class MeasurementTimeline {
    private MeasurementTimeline() {}

    /** Kiểm đủ tham số theo kiểu tần suất (FR-003). */
    public static void validate(ScheduleFrequency frequency) {
        if (frequency == null || frequency.type() == null) throw new InvalidValueException("Thiếu tần suất đo");
        switch (frequency.type()) {
            case CLOCK_TIMES -> {
                if (frequency.times().isEmpty())
                    throw new InvalidValueException("Tần suất mốc giờ cần ít nhất một mốc");
            }
            case EVERY_N_HOURS -> {
                if (frequency.everyHours() == null || frequency.everyHours() < 1) {
                    throw new InvalidValueException("Mỗi N giờ cần N từ 1 trở lên");
                }
            }
            case WEEKDAYS -> {
                if (frequency.weekdays().isEmpty()
                        || frequency.times().isEmpty()
                        || frequency.weekdays().stream().anyMatch(d -> d < 1 || d > 7)) {
                    throw new InvalidValueException("Theo thứ cần các thứ 1–7 và ít nhất một mốc giờ");
                }
            }
        }
    }

    /** Các mốc {@code t} với {@code startAt <= t < endAt} (kết thúc trống là không giới hạn) và {@code from <= t < to}. */
    public static List<Instant> occurrences(
            ScheduleFrequency frequency, Instant startAt, Instant endAt, Instant from, Instant to) {
        var lo = from.isAfter(startAt) ? from : startAt;
        var hi = endAt != null && endAt.isBefore(to) ? endAt : to;
        var out = new ArrayList<Instant>();
        if (!lo.isBefore(hi)) return out;
        if (frequency.type() == com.eldercare.backend.health.api.dto.MeasurementFrequency.EVERY_N_HOURS) {
            var step = Duration.ofHours(frequency.everyHours());
            var anchor = frequency.anchorAt() == null ? startAt : frequency.anchorAt();
            long k = Math.ceilDiv(Duration.between(anchor, lo).toSeconds(), step.toSeconds());
            for (var t = anchor.plus(step.multipliedBy(k)); t.isBefore(hi); t = t.plus(step)) out.add(t);
            return out;
        }
        LocalDate day = lo.atZone(BusinessTime.ZONE).toLocalDate();
        LocalDate last = hi.atZone(BusinessTime.ZONE).toLocalDate();
        for (; !day.isAfter(last); day = day.plusDays(1)) {
            if (frequency.type() == com.eldercare.backend.health.api.dto.MeasurementFrequency.WEEKDAYS
                    && !frequency.weekdays().contains(day.getDayOfWeek().getValue())) continue;
            for (LocalTime time : frequency.times().stream().sorted().toList()) {
                var t = day.atTime(time).atZone(BusinessTime.ZONE).toInstant();
                if (!t.isBefore(lo) && t.isBefore(hi)) out.add(t);
            }
        }
        return out;
    }
}
