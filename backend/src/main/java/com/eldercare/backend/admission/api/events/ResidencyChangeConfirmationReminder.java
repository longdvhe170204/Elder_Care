package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Người đại diện chưa trả lời đề nghị xác nhận quá CFG-M15-05 (FR-043c): nhắc người đại diện một lần và báo Hành chính để
 * liên hệ (gặp trực tiếp, gọi điện); không tự hủy (Q-306). Mức Nhẹ.
 */
public record ResidencyChangeConfirmationReminder(long confirmationId, long requestId, long residentId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(
                RepresentativeNotices.of(
                        "RESIDENCY_CHANGE_CONFIRMATION_REMINDER",
                        new SourceRecord("M02.CHANGE_CONFIRMATION", String.valueOf(confirmationId)),
                        "RESIDENCY_CHANGE_CONFIRMATION_REMINDER:" + confirmationId,
                        residentId,
                        "Nhắc xác nhận điều chỉnh hợp đồng",
                        "Viện đang chờ bạn xác nhận đề nghị điều chỉnh hợp đồng. Mở ứng dụng để xem và trả lời.",
                        at),
                Notices.of(
                        "RESIDENCY_CHANGE_CONFIRMATION_OVERDUE",
                        new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                        "RESIDENCY_CHANGE_CONFIRMATION_OVERDUE:" + confirmationId,
                        NotificationLevel.LOW,
                        residentId,
                        Notices.OFFICE,
                        "Người đại diện chưa trả lời đề nghị xác nhận",
                        "Người đại diện chưa trả lời yêu cầu điều chỉnh hợp đồng. Hành chính liên hệ trực tiếp và ghi xác"
                                + " nhận kèm bằng chứng nếu cần.",
                        at));
    }
}
