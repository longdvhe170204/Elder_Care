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
 * Thực đơn gửi kiểm tra, chờ công bố, hoặc bếp rút lại (FR-065 dòng "Thực đơn gửi kiểm tra, chờ duyệt; nhân viên bếp rút
 * lại"; FR-029): báo Bác sĩ mức Nhẹ.
 */
public record MenuSubmitted(long menuId, LocalDate weekStart, boolean withdrawn, long by, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String type = withdrawn ? "MENU_WITHDRAWN" : "MENU_SUBMITTED";
        String text = withdrawn
                ? "Nhân viên bếp đã rút lại thực đơn tuần " + weekStart
                : "Thực đơn tuần " + weekStart + " chờ Bác sĩ công bố";
        return List.of(MenuNotices.request(
                type,
                menuId,
                "M08." + type + ":" + menuId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                text,
                withdrawn ? "Thực đơn được rút lại" : "Thực đơn chờ công bố",
                by,
                at));
    }
}
