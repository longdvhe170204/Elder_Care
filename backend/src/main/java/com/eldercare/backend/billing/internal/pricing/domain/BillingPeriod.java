package com.eldercare.backend.billing.internal.pricing.domain;

import java.time.*;
import java.util.Objects;
import java.util.Optional;

/** T006/FR-011: khoảng tính phí từ mốc hợp đồng, buổi đầu và mốc dừng do nguồn cung cấp. */
public record BillingPeriod(
        LocalDate startsOn,
        LocalDate endsOn,
        LocalDate admittedOn,
        Instant deathAt,
        Instant firstSessionAt,
        boolean residential) {
    public BillingPeriod {
        Objects.requireNonNull(startsOn);
    }

    public static BillingPeriod residential(
            LocalDate contractStartsOn,
            LocalDate admittedOn,
            LocalDate endingOn,
            LocalDate cancelledOn,
            Instant deathAt) {
        return new BillingPeriod(contractStartsOn, earlier(endingOn, cancelledOn), admittedOn, deathAt, null, true);
    }

    public static Optional<BillingPeriod> dayCare(
            LocalDate contractStartsOn,
            Instant firstSessionAt,
            LocalDate endingOn,
            LocalDate cancelledOn,
            Instant deathAt,
            ZoneId zone) {
        Objects.requireNonNull(contractStartsOn);
        if (firstSessionAt == null) return Optional.empty();
        var firstDay = LocalDate.ofInstant(firstSessionAt, zone);
        if (firstDay.isBefore(contractStartsOn)) {
            throw new IllegalArgumentException("Buổi đầu không được trước ngày bắt đầu hợp đồng");
        }
        return Optional.of(
                new BillingPeriod(firstDay, earlier(endingOn, cancelledOn), null, deathAt, firstSessionAt, false));
    }

    /** Kiểm thời điểm phát sinh, không dùng thời điểm Hành chính lập khoản. */
    public boolean contains(Instant occurredAt, ZoneId zone) {
        return inDateRange(LocalDate.ofInstant(occurredAt, zone))
                && (firstSessionAt == null || !occurredAt.isBefore(firstSessionAt))
                && (deathAt == null || !occurredAt.isAfter(deathAt));
    }

    /** FR-036: ngày qua đời vẫn là ngày lập được phí lưu trú. */
    public boolean containsDay(LocalDate day, ZoneId zone) {
        return inDateRange(day) && (deathAt == null || !day.isAfter(LocalDate.ofInstant(deathAt, zone)));
    }

    public boolean notYetAdmitted(LocalDate day) {
        return residential && (admittedOn == null || day.isBefore(admittedOn));
    }

    private boolean inDateRange(LocalDate day) {
        return !day.isBefore(startsOn) && (endsOn == null || !day.isAfter(endsOn));
    }

    private static LocalDate earlier(LocalDate left, LocalDate right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isBefore(right) ? left : right;
    }
}
