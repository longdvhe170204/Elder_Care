package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Nhắc thiếu bản đồng ý; báo Hành chính, mức Nhẹ (FR-012, FR-054). Khóa sự kiện (residentId, reminderNumber). */
public record MissingConsentReminder(long residentId, int reminderNumber, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "MISSING_CONSENT_REMINDER",
                NotificationLevel.LOW,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "MISSING_CONSENT_REMINDER:" + residentId + ":" + reminderNumber,
                residentId,
                null,
                List.of(Notices.OFFICE),
                Notices.general("Nhắc lần " + reminderNumber + ": người cao tuổi chưa có bản đồng ý chia sẻ dữ liệu."),
                "Thiếu bản đồng ý",
                "Có người cao tuổi chưa có bản đồng ý chia sẻ dữ liệu.",
                at));
    }
}
