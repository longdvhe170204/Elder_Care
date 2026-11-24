package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lệnh Chốt suất đã ghi (FR-033, FR-038; FR-065, Q-445): tuần chưa có thực đơn hoặc chốt sau giờ bữa → nhân viên bếp mức
 * Trung bình, thêm Quản lý viện khi chưa có thực đơn. {@code flags}: {@code NO_MENU}, {@code CLOSED_AFTER_MEAL_TIME}.
 */
public record PortionsClosed(long dailyMealId, long mealId, LocalDate date, Set<String> flags, long by, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (flags.isEmpty()) {
            return List.of();
        }
        List<RecipientGroup> recipients = new ArrayList<>(List.of(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)));
        if (flags.contains("NO_MENU")) {
            recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        }
        return List.of(PortionNotices.request(
                "PORTIONS_CLOSED_WITH_FLAGS",
                dailyMealId,
                "M08.PORTIONS_CLOSED:" + dailyMealId + ":FLAGS",
                NotificationLevel.MEDIUM,
                null,
                recipients,
                "Bữa ngày " + date + " đã chốt suất cần xử lý: " + String.join(", ", flags),
                "Chốt suất cần xử lý",
                by,
                at));
    }
}
