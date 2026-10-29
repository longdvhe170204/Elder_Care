package com.eldercare.backend.notification.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Một người đã được thông báo về bản ghi nguồn ({@code NguoiDaThongBaoView}; FR-035).
 *
 * @param missingLevel yêu cầu thiếu mức, 009 đã coi là Nhẹ (FR-003)
 */
public record NotifiedPersonView(
        long notificationId,
        RecipientType recipientType,
        long recipientId,
        List<String> basis,
        NotificationStatus status,
        List<ChannelResult> channels,
        Optional<Instant> sentAt,
        Optional<Instant> viewedAt,
        Optional<Instant> acknowledgedAt,
        Optional<AcknowledgementMethod> acknowledgementMethod,
        List<CallReminderSummary> callReminders,
        boolean missingLevel) {

    /** Kết quả một lần gửi trên một kênh. */
    public record ChannelResult(String channel, String result, Instant at) {}

    /**
     * Một lượt nhắc gọi của chuỗi gọi chứa thông báo này, kèm người cần gọi của lượt (người nhận, hoặc người thân kế tiếp
     * theo FR-029a) và các kết quả gọi (FR-031, Q-388).
     */
    public record CallReminderSummary(
            long callReminderId,
            RecipientType personToCallType,
            long personToCallId,
            String status,
            List<CallOutcome> results) {}

    /** Một kết quả gọi. */
    public record CallOutcome(String result, Instant calledAt) {}
}
