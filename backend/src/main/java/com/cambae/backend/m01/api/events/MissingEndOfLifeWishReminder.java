package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Nhắc thiếu phiếu nguyện vọng; báo Bác sĩ trực và Điều dưỡng phụ trách, mức Nhẹ (FR-023d, FR-054). Khóa sự kiện
 * (residentId, reminderNumber).
 */
public record MissingEndOfLifeWishReminder(long residentId, int reminderNumber, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "MISSING_END_OF_LIFE_WISH_REMINDER",
                NotificationLevel.LOW,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "MISSING_END_OF_LIFE_WISH_REMINDER:" + residentId + ":" + reminderNumber,
                residentId,
                null,
                List.of(Notices.doctor(), Notices.assignedNurse(residentId)),
                Notices.health("Nhắc lần " + reminderNumber + ": người cao tuổi chưa có phiếu nguyện vọng cuối đời."),
                "Thiếu phiếu nguyện vọng",
                "Có người cao tuổi chưa có phiếu nguyện vọng.",
                at));
    }
}
