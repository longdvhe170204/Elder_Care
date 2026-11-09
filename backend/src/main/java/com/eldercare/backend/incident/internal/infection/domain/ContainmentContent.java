package com.eldercare.backend.incident.internal.infection.domain;

import java.util.Set;

/** Dữ liệu khoanh vùng trên hồ sơ chung, giữ cả thông tin gỡ. */
public record ContainmentContent(
        ContainmentPeriod period, String reason, Set<Long> incidentIds, Long releasedBy, String releaseReason) {
    public ContainmentContent {
        incidentIds = Set.copyOf(incidentIds);
    }
}
