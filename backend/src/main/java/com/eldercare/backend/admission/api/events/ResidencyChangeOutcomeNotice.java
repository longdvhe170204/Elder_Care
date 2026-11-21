package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Kết quả yêu cầu thay đổi lưu trú liên quan tới gia đình (FR-043g, Q-421): Từ chối, Áp dụng không thành, Hủy. Báo mọi
 * người đại diện hiện tại, mức Nhẹ, nội dung loại thay đổi, không kèm giá.
 */
public record ResidencyChangeOutcomeNotice(
        long requestId, long residentId, Outcome outcome, Set<String> changeTypes, Instant at)
        implements HasNotificationRequests {

    public enum Outcome {
        REJECTED,
        APPLY_FAILED,
        CANCELLED
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        String what = "Yêu cầu thay đổi " + RepresentativeNotices.changes(changeTypes);
        String text = switch (outcome) {
            case REJECTED -> what + " không được viện duyệt.";
            case APPLY_FAILED -> what + " không áp dụng được vì điều kiện không còn đáp ứng.";
            case CANCELLED -> what + " đã bị hủy.";
        };
        return List.of(RepresentativeNotices.of(
                "RESIDENCY_CHANGE_OUTCOME",
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "RESIDENCY_CHANGE_OUTCOME:" + requestId + ":" + outcome,
                residentId,
                "Kết quả yêu cầu điều chỉnh hợp đồng",
                text,
                at));
    }
}
