package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.CarePlanItemView;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;

/** C-05/BR-M04-25: chỉ tính mốc kiểm lệnh;không sinh công việc hoặc phân bố giờ tự động. */
public final class CarePlanItemSequence {
    private CarePlanItemSequence() {}

    public static List<Instant> withinShift(CarePlanItemView item, Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) throw new InvalidValueException("Khoảng ca không hợp lệ");
        String frequency = item.frequencyType();
        if (!Set.of("EVERY_N_HOURS", "TIMES_PER_DAY", "DAYS_OF_WEEK", "ONCE_PER_SHIFT")
                .contains(frequency)) throw new InvalidValueException("Tần suất không hợp lệ");
        if (frequency.equals("EVERY_N_HOURS")
                && (item.intervalHours().isEmpty()
                        || item.intervalHours().get() < 1
                        || item.intervalHours().get() > 23
                        || item.startsAt().isEmpty()))
            throw new InvalidValueException("Mỗi N giờ cần N từ1đến23 và mốc bắt đầu");
        if (frequency.equals("ONCE_PER_SHIFT")) return List.of(from);
        if (item.startsAt().isEmpty() && item.times().isEmpty()) {
            return !frequency.equals("DAYS_OF_WEEK")
                            || selected(item, from.atZone(BusinessTime.ZONE).toLocalDate())
                    ? List.of(from)
                    : List.of();
        }
        var result = new TreeSet<Instant>();
        var last = to.minusNanos(1).atZone(BusinessTime.ZONE).toLocalDate();
        for (var date = from.atZone(BusinessTime.ZONE).toLocalDate(); !date.isAfter(last); date = date.plusDays(1)) {
            if (frequency.equals("DAYS_OF_WEEK") && !selected(item, date)) continue;
            if (frequency.equals("EVERY_N_HOURS")) {
                for (var at = date.atTime(item.startsAt().orElseThrow());
                        at.toLocalDate().equals(date);
                        at = at.plusHours(item.intervalHours().orElseThrow())) {
                    include(result, at.atZone(BusinessTime.ZONE).toInstant(), from, to);
                }
            } else {
                var times = item.times().isEmpty() ? List.of(item.startsAt().orElseThrow()) : item.times();
                for (var time : times)
                    include(result, date.atTime(time).atZone(BusinessTime.ZONE).toInstant(), from, to);
            }
        }
        return List.copyOf(result);
    }

    private static boolean selected(CarePlanItemView item, LocalDate date) {
        return item.weekdays().isPresent()
                && (item.weekdays().get() & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
    }

    private static void include(Set<Instant> result, Instant at, Instant from, Instant to) {
        if (!at.isBefore(from) && at.isBefore(to)) result.add(at);
    }
}
