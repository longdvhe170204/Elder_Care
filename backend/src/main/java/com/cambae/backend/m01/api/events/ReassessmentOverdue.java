package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Quá hạn đánh giá lại; nhắc Bác sĩ trực mức Trung bình (H-14; FR-041, FR-054). Khóa sự kiện (residentId, dueAt); thời
 * điểm phát sinh là hạn.
 */
public record ReassessmentOverdue(long residentId, Instant dueAt) implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "REASSESSMENT_OVERDUE",
                NotificationLevel.MEDIUM,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "REASSESSMENT_OVERDUE:" + residentId + ":" + dueAt.toEpochMilli(),
                residentId,
                null,
                List.of(Notices.doctor()),
                Notices.health("Đã quá hạn đánh giá lại người cao tuổi."),
                "Quá hạn đánh giá lại",
                "Có người cao tuổi đã quá hạn đánh giá lại.",
                dueAt));
    }
}
