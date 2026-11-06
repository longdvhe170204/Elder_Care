package com.eldercare.backend.incident.internal.incident.domain;

import java.time.Instant;

/**
 * Khoảng theo dõi sau ngã (data-model {@code khoang_theo_doi_sau_nga}, R-15 → hồ sơ chung); khoảng đo chốt lúc tạo
 * (FR-051, FR-052).
 */
public record FallMonitoringContent(
        long incidentId, Instant anchor, Instant endsAt, long intervalMinutes, String reason, Long endedBy) {

    /** Kết thúc sớm (Bác sĩ, có lý do) hoặc hết thời gian ({@code endedBy} rỗng). */
    public FallMonitoringContent ended(String reason, Long endedBy) {
        return new FallMonitoringContent(incidentId, anchor, endsAt, intervalMinutes, reason, endedBy);
    }
}
