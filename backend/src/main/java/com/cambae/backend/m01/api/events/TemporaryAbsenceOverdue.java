package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;

/**
 * Tạm vắng quá mốc dự kiến cộng thời gian chờ; báo Trưởng tầng của tầng và Hành chính, mức Trung bình (FR-050,
 * FR-054). Chưa biết tầng ({@code floorAreaId} rỗng, ví dụ chưa có phân bổ giường) thì báo Hành chính và Quản lý viện.
 * Khóa sự kiện (absenceId, expectedReturnAt), nên gia hạn đổi mốc thì cảnh báo lại.
 */
public record TemporaryAbsenceOverdue(
        long residentId, long absenceId, Instant expectedReturnAt, Long floorAreaId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = floorAreaId == null
                ? List.of(Notices.OFFICE, Notices.MANAGER)
                : List.of(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()), Notices.OFFICE);
        return List.of(Notices.of(
                "TEMPORARY_ABSENCE_OVERDUE",
                NotificationLevel.MEDIUM,
                new SourceRecord("M01.ABSENCE", String.valueOf(absenceId)),
                "TEMPORARY_ABSENCE_OVERDUE:" + absenceId + ":" + expectedReturnAt.toEpochMilli(),
                residentId,
                floorAreaId,
                recipients,
                Notices.general("Người cao tuổi tạm vắng đã quá giờ dự kiến trở về mà chưa được ghi nhận trở về."),
                "Tạm vắng quá hạn trở về",
                "Có người cao tuổi tạm vắng quá hạn trở về.",
                at));
    }
}
