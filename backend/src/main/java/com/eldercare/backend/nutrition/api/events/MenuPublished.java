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
 * Thực đơn Công bố hoặc Đã áp dụng ngay khi tuần đã bắt đầu (FR-065 dòng "Thực đơn Công bố"; FR-028): báo nhân viên bếp mức
 * Nhẹ. Thực đơn chung cho 012 đọc qua {@code GeneralMenuApi}.
 */
public record MenuPublished(long menuId, LocalDate weekStart, boolean applied, long by, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(MenuNotices.request(
                "MENU_PUBLISHED",
                menuId,
                "M08.MENU_PUBLISHED:" + menuId,
                NotificationLevel.LOW,
                List.of(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)),
                "Thực đơn tuần " + weekStart + " đã được Bác sĩ công bố",
                "Thực đơn đã công bố",
                by,
                at));
    }
}
