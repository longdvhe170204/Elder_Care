package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Tới ngày xem lại quyết định giữ mức chăm sóc cũ khi gia đình không đồng ý đổi mức (001 FR-036b, Q-424): nhắc Bác sĩ và
 * Hành chính một lần, mức Nhẹ.
 */
public record CareLevelKeepReviewDue(long residentId, long decisionId, Instant at) implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "CARE_LEVEL_KEEP_REVIEW_DUE",
                NotificationLevel.LOW,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "CARE_LEVEL_KEEP_REVIEW_DUE:" + decisionId,
                residentId,
                null,
                List.of(Notices.doctor(), Notices.OFFICE),
                Notices.general("Tới ngày xem lại quyết định giữ mức chăm sóc cũ theo thỏa thuận lựa chọn có rủi ro."),
                "Xem lại quyết định giữ mức chăm sóc",
                "Tới ngày xem lại quyết định giữ mức chăm sóc cũ.",
                at));
    }
}
