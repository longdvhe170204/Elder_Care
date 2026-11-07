package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;

/** Thời gian áp dụng khoanh vùng từ khi đặt đến khi gỡ. */
public record ContainmentPeriod(ContainmentScope scope, Instant startedAt, Instant releasedAt) {

    public boolean includes(Instant at) {
        return !at.isBefore(startedAt) && (releasedAt == null || at.isBefore(releasedAt));
    }
}
