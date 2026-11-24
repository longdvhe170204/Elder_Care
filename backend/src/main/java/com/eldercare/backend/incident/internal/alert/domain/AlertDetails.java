package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Cảnh báo kèm phần đọc cho màn hình (api-007 {@code Alert}): họ tên người phụ trách, nguồn, lệnh được phép. */
public record AlertDetails(
        AlertRecord alert,
        String assigneeName,
        List<Source> sources,
        Set<String> allowedCommands,
        Press press,
        String floorName,
        List<Report> reports,
        Assist assist,
        String closedByName,
        List<FollowUp> followUps) {
    public AlertDetails {
        sources = List.copyOf(sources);
        allowedCommands = Set.copyOf(allowedCommands);
        reports = List.copyOf(reports);
        followUps = List.copyOf(followUps);
        if (assist == null) assist = Assist.NONE;
    }

    public AlertDetails(AlertRecord alert, String assigneeName, List<Source> sources, Set<String> allowedCommands) {
        this(alert, assigneeName, sources, allowedCommands, Press.NONE, null, List.of());
    }

    public AlertDetails(
            AlertRecord alert,
            String assigneeName,
            List<Source> sources,
            Set<String> allowedCommands,
            Press press,
            String floorName,
            List<Report> reports) {
        this(alert, assigneeName, sources, allowedCommands, press, floorName, reports, Assist.NONE, null, List.of());
    }

    /**
     * Phần của yêu cầu hỗ trợ (đề xuất 2026-11-23): người xem có phải người gửi, người gửi ngoài ca, người đã bấm "Tôi đến
     * giúp" (cả với SOS), tin người gửi nhắn thêm.
     */
    public record Assist(boolean mine, boolean senderOffShift, List<String> helpers, List<Message> messages) {
        public static final Assist NONE = new Assist(false, false, List.of(), List.of());

        public Assist {
            helpers = List.copyOf(helpers);
            messages = List.copyOf(messages);
        }
    }

    /** Một tin người gửi nhắn thêm. */
    public record Message(String by, String text, Instant at) {}

    /** Một lần báo của nhân viên gộp vào cảnh báo thủ công đang mở (FR-030): người báo, mô tả, thời điểm. */
    public record Report(String reportedBy, String description, Instant at) {}

    public AlertDetails(
            AlertRecord alert, String assigneeName, List<Source> sources, Set<String> allowedCommands, Press press) {
        this(alert, assigneeName, sources, allowedCommands, press, null, List.of());
    }

    /** Một lần ghi kết quả theo dõi (FR-087, Q-443): nội dung, thời điểm, người ghi. */
    public record FollowUp(String content, Instant at, String recordedBy) {}

    /** Người bấm (nút hỗ trợ), loại sự việc và nơi đã chọn của cảnh báo từ nút SOS, nút hỗ trợ (FR-047f, FR-047i). */
    public record Press(
            String pressedBy,
            String eventType,
            String chosenPlace,
            String placeNote,
            AlertLocationView latestLocation) {
        public static final Press NONE = new Press(null, null, null, null, null);

        public Press(String pressedBy, String eventType, String chosenPlace) {
            this(pressedBy, eventType, chosenPlace, null, null);
        }

        public Press withLocation(AlertLocationView location) {
            return new Press(pressedBy, eventType, chosenPlace, placeNote, location);
        }
    }

    /** Một phát sinh đã gộp (dòng {@code M05.ALERT_SOURCE}, FR-027, FR-030). */
    public record Source(String sourceType, Long sourceId, String level, Instant occurredAt, Instant resolvedAt) {}
}
