package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/**
 * Món trong thực đơn chưa hết tuần không còn dùng được (FR-002, Edge Cases; FR-065 dòng "Chế độ ăn sửa danh sách hạn chế làm
 * món trong thực đơn chưa hết tuần vi phạm FR-005; món bị Ngừng hiệu lực khi đang có trong thực đơn đã công bố"): báo Bác sĩ
 * và nhân viên bếp mức Nhẹ để Bếp Đổi món; thực đơn không tự đổi. {@code reason}: {@code DIET_RESTRICTION} (nguồn là chế độ
 * ăn), {@code DISH_DEACTIVATED} (nguồn là món); {@code count}: số dòng thực đơn bị ảnh hưởng.
 */
public record MenuDishViolation(String reason, long sourceId, String sourceName, int count, Long by, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String text = "DIET_RESTRICTION".equals(reason)
                ? "Chế độ ăn \"" + sourceName + "\" vừa đổi hạn chế: " + count
                        + " món trong thực đơn chưa hết tuần không còn phù hợp, cần Đổi món"
                : "Món \"" + sourceName + "\" đã Ngừng hiệu lực nhưng còn " + count
                        + " lần trong thực đơn đã công bố, cần Đổi món";
        return List.of(MenuNotices.request(
                "MENU_DISH_VIOLATION",
                sourceId,
                "M08.MENU_DISH_VIOLATION:" + reason + ":" + sourceId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                List.of(
                        new RecipientGroup.FacilityRole(Role.DOCTOR),
                        new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)),
                text,
                "Thực đơn cần đổi món",
                by,
                at));
    }
}
