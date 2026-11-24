package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Nhân viên bếp gửi duyệt thực đơn tuần (FR-065 dòng "Thực đơn gửi duyệt"; FR-029, Q-445): báo Quản lý viện mức Nhẹ. */
public record MenuSubmitted(long menuId, LocalDate weekStart, long by, Instant at) implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(MenuNotices.request(
                "MENU_SUBMITTED",
                menuId,
                "M08.MENU_SUBMITTED:" + menuId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                "Thực đơn tuần " + weekStart + " chờ Quản lý viện duyệt",
                "Thực đơn chờ duyệt",
                by,
                at));
    }
}
