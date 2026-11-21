package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Người duyệt trả yêu cầu thay đổi lưu trú về Nháp kèm lý do (FR-043e, Q-418): báo người lập (nhân viên) và Hành chính,
 * mức Nhẹ. Xác nhận người đại diện cũ đã bị bỏ, phải xác nhận lại trước khi gửi duyệt.
 */
public record ResidencyChangeReturnedToDraft(
        long requestId, long residentId, Long requesterStaffId, String reason, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = new ArrayList<>(List.of(Notices.OFFICE));
        if (requesterStaffId != null) {
            recipients.add(new RecipientGroup.Staff(requesterStaffId, "REQUESTER"));
        }
        String text = "Yêu cầu điều chỉnh hợp đồng được trả về Nháp để sửa: " + reason
                + ". Xác nhận với người đại diện trước đó đã bị bỏ, cần xác nhận lại rồi gửi duyệt.";
        return List.of(new NotificationRequest(
                "M02",
                "RESIDENCY_CHANGE_RETURNED_TO_DRAFT",
                SourceGroup.OTHER,
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "RESIDENCY_CHANGE_RETURNED_TO_DRAFT:" + requestId + ":" + at.toEpochMilli(),
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of("Yêu cầu điều chỉnh hợp đồng được trả về Nháp"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
