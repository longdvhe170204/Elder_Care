package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Bác sĩ trả lại thực đơn (FR-065 dòng "Thực đơn bị trả lại (kèm lý do)"; FR-029): báo nhân viên bếp đã gửi mức Trung
 * bình, kèm lý do. {@code submitterStaffId} là hồ sơ nhân viên của người gửi.
 */
public record MenuReturned(long menuId, LocalDate weekStart, String reason, long submitterStaffId, long by, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(MenuNotices.request(
                "MENU_RETURNED",
                menuId,
                "M08.MENU_RETURNED:" + menuId + ":" + at.toEpochMilli(),
                NotificationLevel.MEDIUM,
                List.of(new RecipientGroup.Staff(submitterStaffId, "MENU_SUBMITTER")),
                "Thực đơn tuần " + weekStart + " bị trả lại: " + reason,
                "Thực đơn bị trả lại",
                by,
                at));
    }
}
