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
 * Tới mốc chốt (giờ bữa − CFG-M08-01) mà bữa chưa chốt (FR-033; FR-065): nhắc nhân viên bếp mức Trung bình. Hệ thống không
 * tự chốt. Gửi mọi nhân viên bếp: chưa có nhóm người nhận "bếp đang trong ca" (plan ghi chú Phase 5).
 */
public record PortionClosingReminder(long dailyMealId, String mealName, LocalDate date, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(PortionNotices.request(
                "PORTION_CLOSING_DUE",
                dailyMealId,
                "M08.PORTION_CLOSING_DUE:" + dailyMealId,
                NotificationLevel.MEDIUM,
                null,
                List.of(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)),
                "Đã tới mốc chốt suất " + mealName + " ngày " + date + " mà bữa chưa được chốt",
                "Nhắc chốt suất",
                null,
                at));
    }
}
