package com.eldercare.backend.staffing.shift.internal.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/** BR-M09-03: độ dài chuỗi chứa ca đang xếp; khoảng [from,to), tiếp giáp không có khoảng nghỉ. */
public final class ContinuousShiftChain {
    private ContinuousShiftChain() {}

    public record Interval(Instant from, Instant to) {
        public Interval {
            if (from == null || to == null || !from.isBefore(to))
                throw new IllegalArgumentException("Khoảng ca phải có độ dài dương");
        }
    }

    public static Duration duration(Collection<Interval> existing, Interval candidate) {
        var ordered = new ArrayList<>(existing);
        ordered.add(candidate);
        ordered.sort(Comparator.comparing(Interval::from));
        Instant start = ordered.getFirst().from();
        Instant end = ordered.getFirst().to();
        for (Interval next : ordered.subList(1, ordered.size())) {
            if (next.from().isAfter(end)) {
                if (!candidate.from().isBefore(start) && !candidate.to().isAfter(end))
                    return Duration.between(start, end);
                start = next.from();
                end = next.to();
            } else if (next.to().isAfter(end)) {
                end = next.to();
            }
        }
        return Duration.between(start, end);
    }
}
