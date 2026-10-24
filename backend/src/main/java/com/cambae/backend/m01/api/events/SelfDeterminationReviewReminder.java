package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Nhắc xem lại nhận định tự quyết (FR-023i, Q-360); báo Bác sĩ đã nhận định và Điều dưỡng phụ trách, mức Nhẹ. Không rõ
 * hồ sơ nhân viên của Bác sĩ đã nhận định ({@code assessingDoctorStaffId} rỗng) thì báo Bác sĩ trực. Khóa sự kiện là
 * phiếu nguyện vọng, nên mỗi phiếu chỉ nhắc một lần.
 */
public record SelfDeterminationReviewReminder(long residentId, long wishId, Long assessingDoctorStaffId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        RecipientGroup doctor = assessingDoctorStaffId == null
                ? Notices.doctor()
                : new RecipientGroup.Staff(assessingDoctorStaffId, "ASSESSING_DOCTOR");
        return List.of(Notices.of(
                "SELF_DETERMINATION_REVIEW_REMINDER",
                NotificationLevel.LOW,
                new SourceRecord("M01.END_OF_LIFE_WISH", String.valueOf(wishId)),
                "SELF_DETERMINATION_REVIEW_REMINDER:" + wishId,
                residentId,
                null,
                List.of(doctor, Notices.assignedNurse(residentId)),
                Notices.health("Đến hạn xem lại nhận định tự quyết ghi trên phiếu nguyện vọng."),
                "Xem lại nhận định tự quyết",
                "Có phiếu nguyện vọng đến hạn xem lại nhận định.",
                at));
    }
}
