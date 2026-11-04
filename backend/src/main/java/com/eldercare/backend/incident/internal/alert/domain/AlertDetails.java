package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Cảnh báo kèm phần đọc cho màn hình (api-007 {@code Alert}): họ tên người phụ trách, nguồn, lệnh được phép. */
public record AlertDetails(AlertRecord alert, String assigneeName, List<Source> sources, Set<String> allowedCommands) {
    public AlertDetails {
        sources = List.copyOf(sources);
        allowedCommands = Set.copyOf(allowedCommands);
    }

    /** Một phát sinh đã gộp (dòng {@code M05.ALERT_SOURCE}, FR-027, FR-030). */
    public record Source(String sourceType, Long sourceId, String level, Instant occurredAt, Instant resolvedAt) {}
}
