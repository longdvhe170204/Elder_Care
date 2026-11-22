package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Phụ lục chưa ký quá CFG-M02-18 kể từ ngày duyệt (FR-043d, Q-420): báo Quản lý viện đúng một lần, mức Trung bình. Gia đình
 * đổi ý thì hủy yêu cầu (FR-043f).
 */
public record AddendumSignatureOverdue(long addendumId, long requestId, long residentId, int addendumNo, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "ADDENDUM_SIGNATURE_OVERDUE",
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "ADDENDUM_SIGNATURE_OVERDUE:" + addendumId,
                NotificationLevel.MEDIUM,
                residentId,
                Notices.MANAGER,
                "Phụ lục số %02d quá hạn ký".formatted(addendumNo),
                "Phụ lục hợp đồng đã duyệt quá lâu mà người đại diện chưa ký. Liên hệ gia đình; gia đình đổi ý thì hủy"
                        + " yêu cầu điều chỉnh hợp đồng.",
                at));
    }
}
