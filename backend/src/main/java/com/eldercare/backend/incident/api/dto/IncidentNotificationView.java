package com.eldercare.backend.incident.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Người nhận, kênh, mốc gửi/xem và chuỗi nhắc gọi theo bản ghi nguồn (FR-040, FR-048b, Q-388). */
public record IncidentNotificationView(
        String recipientType,
        long recipientId,
        String channel,
        Optional<Instant> sentAt,
        Optional<Instant> viewedAt,
        List<CallReminderSummary> callReminders) {

    public IncidentNotificationView {
        callReminders = List.copyOf(callReminders);
    }

    public IncidentNotificationView(
            String recipientType,
            long recipientId,
            String channel,
            Optional<Instant> sentAt,
            Optional<Instant> viewedAt) {
        this(recipientType, recipientId, channel, sentAt, viewedAt, List.of());
    }

    /** Một lượt nhắc gọi, có thể dành cho người khác với người nhận thông báo. */
    public record CallReminderSummary(
            long callReminderId,
            String personToCallType,
            long personToCallId,
            String status,
            List<CallOutcome> results) {

        public CallReminderSummary {
            results = List.copyOf(results);
        }
    }

    /** Kết quả và thời điểm của một cuộc gọi. */
    public record CallOutcome(String result, Instant calledAt) {}
}
