package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Cần đánh giá lại, chưa quá hạn; nhắc Bác sĩ trực mức Nhẹ (H-14; FR-039 → FR-041, FR-054). Khóa sự kiện (residentId,
 * noticeId). Căn cứ chỉ ở nội dung đầy đủ gắn loại sức khỏe.
 */
public record ReassessmentDue(long residentId, long noticeId, String basis, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "REASSESSMENT_DUE",
                NotificationLevel.LOW,
                new SourceRecord("M01.REASSESSMENT_NOTICE", String.valueOf(noticeId)),
                "REASSESSMENT_DUE:" + residentId + ":" + noticeId,
                residentId,
                null,
                List.of(Notices.doctor()),
                Notices.health("Người cao tuổi cần được đánh giá lại (căn cứ: " + basis + ")."),
                "Cần đánh giá lại",
                "Có người cao tuổi cần được đánh giá lại.",
                at));
    }
}
