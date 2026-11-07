package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Cảnh báo kèm phần đọc cho màn hình (api-007 {@code Alert}): họ tên người phụ trách, nguồn, lệnh được phép. */
public record AlertDetails(
        AlertRecord alert, String assigneeName, List<Source> sources, Set<String> allowedCommands, Press press) {
    public AlertDetails {
        sources = List.copyOf(sources);
        allowedCommands = Set.copyOf(allowedCommands);
    }

    public AlertDetails(AlertRecord alert, String assigneeName, List<Source> sources, Set<String> allowedCommands) {
        this(alert, assigneeName, sources, allowedCommands, Press.NONE);
    }

    /** Người bấm (nút hỗ trợ), loại sự việc và nơi đã chọn của cảnh báo từ nút SOS, nút hỗ trợ (FR-047f, FR-047i). */
    public record Press(String pressedBy, String eventType, String chosenPlace, AlertLocationView latestLocation) {
        public static final Press NONE = new Press(null, null, null, null);

        public Press(String pressedBy, String eventType, String chosenPlace) {
            this(pressedBy, eventType, chosenPlace, null);
        }

        public Press withLocation(AlertLocationView location) {
            return new Press(pressedBy, eventType, chosenPlace, location);
        }
    }

    /** Một phát sinh đã gộp (dòng {@code M05.ALERT_SOURCE}, FR-027, FR-030). */
    public record Source(String sourceType, Long sourceId, String level, Instant occurredAt, Instant resolvedAt) {}
}
