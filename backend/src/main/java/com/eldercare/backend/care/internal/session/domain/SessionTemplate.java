package com.eldercare.backend.care.internal.session.domain;

import com.eldercare.backend.care.internal.activity.domain.TemplateVersion;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 014 T007 (research L-04 {@code BuoiTheoMau}; FR-005, FR-009): hàm thuần trả thời điểm bắt đầu, kết thúc của các buổi
 * theo mẫu trong khoảng ngày; mỗi ngày dùng phiên bản có ngày áp dụng gần nhất không sau ngày đó.
 */
public final class SessionTemplate {
    private SessionTemplate() {}

    public record VersionFrom(LocalDate effectiveFrom, TemplateVersion version) {}

    public record Slot(Instant startsAt, Instant endsAt) {}

    public static List<Slot> slots(List<VersionFrom> versions, LocalDate from, LocalDate to) {
        var ordered = versions.stream()
                .sorted(Comparator.comparing(VersionFrom::effectiveFrom))
                .toList();
        var result = new ArrayList<Slot>();
        for (var day = from; !day.isAfter(to); day = day.plusDays(1)) {
            var current = inEffect(ordered, day);
            if (current != null && matches(current, day)) result.add(slot(current, day));
        }
        return List.copyOf(result);
    }

    private static TemplateVersion inEffect(List<VersionFrom> ordered, LocalDate day) {
        TemplateVersion found = null;
        for (var v : ordered) if (!v.effectiveFrom().isAfter(day)) found = v.version();
        return found;
    }

    private static boolean matches(TemplateVersion v, LocalDate day) {
        if (day.isBefore(v.startDate())) return false;
        if (v.endDate() != null && day.isAfter(v.endDate())) return false;
        if (v.daysOfWeek() != null && !v.daysOfWeek().isEmpty())
            return v.daysOfWeek().contains(day.getDayOfWeek().getValue());
        return v.intervalDays() != null
                && v.intervalDays() > 0
                && ChronoUnit.DAYS.between(v.startDate(), day) % v.intervalDays() == 0;
    }

    private static Slot slot(TemplateVersion v, LocalDate day) {
        return new Slot(
                day.atTime(LocalTime.parse(v.startTime()))
                        .atZone(BusinessTime.ZONE)
                        .toInstant(),
                day.atTime(LocalTime.parse(v.endTime()))
                        .atZone(BusinessTime.ZONE)
                        .toInstant());
    }
}
