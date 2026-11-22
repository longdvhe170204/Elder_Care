package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Bác sĩ lập yêu cầu thay đổi lưu trú ở Nháp: Hành chính xác nhận với người đại diện, đính kèm bằng chứng rồi gửi Quản lý
 * viện duyệt; mức Nhẹ (chủ dự án yêu cầu 2026-11-20).
 */
public record ResidencyChangeNeedsConfirmation(long requestId, long residentId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "RESIDENCY_CHANGE_NEEDS_CONFIRMATION",
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "RESIDENCY_CHANGE_NEEDS_CONFIRMATION:" + requestId,
                NotificationLevel.LOW,
                residentId,
                Notices.OFFICE,
                "Yêu cầu của Bác sĩ cần xác nhận với người đại diện",
                "Bác sĩ lập yêu cầu điều chỉnh hợp đồng. Hành chính xác nhận với người đại diện, đính kèm bằng chứng rồi"
                        + " gửi Quản lý viện duyệt.",
                at));
    }
}
