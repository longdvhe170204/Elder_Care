package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Nhắc còn phiếu nguyện vọng tự khai do Hành chính lập chưa được kiểm chứng (Q-412, BR-M01-18, FR-023r); báo Bác sĩ trực
 * và Điều dưỡng phụ trách, mức Nhẹ. Khóa sự kiện (residentId, reminderNumber).
 */
public record UnverifiedWishDraftReminder(long residentId, int reminderNumber, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "UNVERIFIED_WISH_DRAFT_REMINDER",
                NotificationLevel.LOW,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "UNVERIFIED_WISH_DRAFT_REMINDER:" + residentId + ":" + reminderNumber,
                residentId,
                null,
                List.of(Notices.doctor(), Notices.assignedNurse(residentId)),
                Notices.health("Nhắc lần "
                        + reminderNumber
                        + ": còn phiếu nguyện vọng tự khai của gia đình chưa được kiểm chứng."),
                "Phiếu nguyện vọng chưa kiểm chứng",
                "Có phiếu nguyện vọng tự khai chưa được Bác sĩ, Điều dưỡng kiểm chứng.",
                at));
    }
}
