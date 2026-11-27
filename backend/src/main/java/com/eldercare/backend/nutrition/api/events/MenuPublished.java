package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Quản lý viện công bố thực đơn, hoặc Đã áp dụng ngay khi tuần đã bắt đầu (FR-065 dòng "Thực đơn Công bố"; FR-028, Q-445):
 * báo nhân viên bếp người gửi mức Nhẹ (không rõ người gửi thì mọi nhân viên bếp). Thực đơn chung đọc qua
 * {@code GeneralMenuApi}.
 */
public record MenuPublished(
        long menuId, LocalDate weekStart, boolean applied, Long submitterStaffId, long by, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(MenuNotices.request(
                "MENU_PUBLISHED",
                menuId,
                "M08.MENU_PUBLISHED:" + menuId,
                NotificationLevel.LOW,
                List.of(
                        submitterStaffId == null
                                ? new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)
                                : new RecipientGroup.Staff(submitterStaffId, "MENU_SUBMITTER")),
                "Thực đơn tuần " + weekStart + " đã được Quản lý viện công bố",
                "Thực đơn đã công bố",
                by,
                at));
    }
}
