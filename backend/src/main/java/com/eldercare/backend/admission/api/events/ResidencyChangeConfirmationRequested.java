package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Hành chính gửi yêu cầu thay đổi lưu trú cho người đại diện xác nhận trên ứng dụng (FR-043c): báo người đại diện, mức
 * Nhẹ, nội dung loại thay đổi và ngày hiệu lực mong muốn (giá xem trong ứng dụng). {@code confirmationId} là lần gửi,
 * nguồn của thông báo để ứng dụng mở đúng màn xác nhận.
 */
public record ResidencyChangeConfirmationRequested(
        long confirmationId,
        long requestId,
        long residentId,
        Set<String> changeTypes,
        LocalDate requestedEffectiveDate,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String text = "Viện đề nghị thay đổi " + RepresentativeNotices.changes(changeTypes) + " từ ngày "
                + RepresentativeNotices.day(requestedEffectiveDate)
                + ". Mở ứng dụng để xem nội dung, chi phí và xác nhận.";
        return List.of(RepresentativeNotices.of(
                "RESIDENCY_CHANGE_CONFIRMATION_REQUESTED",
                new SourceRecord("M02.CHANGE_CONFIRMATION", String.valueOf(confirmationId)),
                "RESIDENCY_CHANGE_CONFIRMATION_REQUESTED:" + confirmationId,
                residentId,
                "Đề nghị xác nhận điều chỉnh hợp đồng",
                text,
                at));
    }
}
