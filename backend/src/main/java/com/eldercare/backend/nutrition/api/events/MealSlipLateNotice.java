package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;

/**
 * Phiếu chưa giao quá giờ bữa + ngưỡng CFG-M08-04 (FR-048a, BR-M08-12; FR-065): Trưởng tầng của tầng/khu (chưa có thì Người
 * phụ trách ca, cách thay mặc định của 009) và nhân viên bếp, mức Nhẹ, một lần mỗi phiếu.
 */
public record MealSlipLateNotice(long slipId, long dailyMealId, long floorAreaId, String mealName, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(PortionNotices.request(
                "MEAL_SLIP_NOT_DELIVERED",
                dailyMealId,
                "M08.MEAL_SLIP_LATE:" + slipId,
                NotificationLevel.LOW,
                null,
                List.of(
                        new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)),
                "Phiếu " + mealName + " của tầng/khu chưa được giao quá ngưỡng giao trễ",
                "Phiếu chưa giao",
                null,
                at));
    }
}
