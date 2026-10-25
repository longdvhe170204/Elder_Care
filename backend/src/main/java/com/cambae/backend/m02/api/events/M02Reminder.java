package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Lần nhắc Hành chính của M02, mức Nhẹ (FR-017, FR-021, FR-026, FR-029, FR-066a, FR-072, FR-076). {@code type} theo
 * glossary mục 10.6 (ví dụ {@code CONTRACT_EXPIRING}); {@code on} là ngày nhắc nên mỗi ngày một thông báo.
 */
public record M02Reminder(String type, long subjectId, LocalDate on, Instant at) implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "M02_REMINDER_" + type,
                new SourceRecord("M02." + type, String.valueOf(subjectId)),
                "M02_REMINDER:" + type + ":" + subjectId + ":" + on,
                NotificationLevel.LOW,
                null,
                Notices.OFFICE,
                "Nhắc việc tiếp nhận và lưu trú",
                "Có việc cần Hành chính xử lý (" + type + ").",
                at));
    }
}
