package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Người đại diện đồng ý hoặc không đồng ý yêu cầu thay đổi lưu trú trên ứng dụng (FR-043c): báo Hành chính, mức Nhẹ. Đồng
 * ý thì Hành chính gửi Quản lý viện duyệt; không đồng ý thì xem lý do trên yêu cầu, sửa hoặc hủy.
 */
public record ResidencyChangeConfirmationAnswered(long requestId, long residentId, boolean accepted, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String result = accepted ? "ACCEPTED" : "DECLINED";
        return List.of(Notices.of(
                "RESIDENCY_CHANGE_CONFIRMATION_" + result,
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "RESIDENCY_CHANGE_CONFIRMATION_" + result + ":" + requestId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                residentId,
                Notices.OFFICE,
                accepted
                        ? "Người đại diện đồng ý điều chỉnh hợp đồng"
                        : "Người đại diện không đồng ý điều chỉnh hợp đồng",
                accepted
                        ? "Người đại diện đã đồng ý yêu cầu điều chỉnh hợp đồng. Hành chính gửi Quản lý viện duyệt."
                        : "Người đại diện không đồng ý yêu cầu điều chỉnh hợp đồng. Xem lý do trên yêu cầu để sửa hoặc hủy.",
                at));
    }
}
