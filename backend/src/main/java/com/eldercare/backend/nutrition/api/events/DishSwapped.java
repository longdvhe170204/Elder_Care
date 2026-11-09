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
 * Nhân viên bếp Đổi món trên thực đơn Công bố, Đã áp dụng (FR-031, BR-M08-08; research M-07): báo Bác sĩ mức Nhẹ, Trung
 * bình khi bữa bị đổi trong ngày. {@code oldDishId} rỗng là thêm món, {@code newDishId} rỗng là bớt món. Phát sinh cho bữa đã chốt có ở phase chốt suất (FR-039).
 */
public record DishSwapped(
        long menuId,
        long swapId,
        LocalDate date,
        long mealId,
        long dietId,
        Long oldDishId,
        Long newDishId,
        boolean sameDay,
        long by,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(MenuNotices.request(
                "DISH_SWAPPED",
                menuId,
                "M08.DISH_SWAPPED:" + swapId,
                sameDay ? NotificationLevel.MEDIUM : NotificationLevel.LOW,
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                "Nhân viên bếp đã đổi món trong thực đơn ngày " + date,
                "Đổi món trong thực đơn",
                by,
                at));
    }
}
