package com.eldercare.backend.billing.internal.period.domain;

import java.time.*;
import java.time.temporal.ChronoUnit;

/** Nhịp kỹ thuật T007; DAILY tới hạn lúc 00:20 theo giờ nghiệp vụ. */
public enum CostJobCadence {
    DAILY(1),
    HOURLY(2);

    private final long markerId;

    CostJobCadence(long markerId) {
        this.markerId = markerId;
    }

    public long markerId() {
        return markerId;
    }

    public Instant latest(Instant now, ZoneId zone) {
        var local = now.atZone(zone);
        if (this == HOURLY) return local.truncatedTo(ChronoUnit.HOURS).toInstant();
        var due = local.toLocalDate().atTime(0, 20).atZone(zone);
        return (due.toInstant().isAfter(now) ? due.minusDays(1) : due).toInstant();
    }

    public Instant next(Instant slot, ZoneId zone) {
        var local = slot.atZone(zone);
        return (this == DAILY ? local.plusDays(1) : local.plusHours(1)).toInstant();
    }
}
