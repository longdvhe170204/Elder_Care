package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lệnh Chốt suất đã ghi (FR-033 → FR-038, FR-014; FR-065): có suất thiếu món thay thế, chưa có món, tuần chưa có thực đơn,
 * chốt sau giờ bữa → Bác sĩ, bếp mức Trung bình (Quản lý viện khi chưa có thực đơn); người chưa gán chế độ ăn, suất tự
 * dùng món an toàn → Bác sĩ mức Nhẹ; món an toàn cũng xung đột → điều dưỡng phụ trách người đó mức Trung bình.
 * {@code flags}: {@code NO_MENU}, {@code CLOSED_AFTER_MEAL_TIME}, {@code MISSING_SUBSTITUTE_DISH}, {@code NO_DISH_IN_MENU}.
 */
public record PortionsClosed(
        long dailyMealId,
        long mealId,
        LocalDate date,
        Set<String> flags,
        int residentsWithoutDiet,
        int safeDishApplied,
        List<Long> safeDishConflicts,
        long by,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<NotificationRequest> out = new ArrayList<>();
        String key = "M08.PORTIONS_CLOSED:" + dailyMealId;
        if (!flags.isEmpty()) {
            List<RecipientGroup> recipients = new ArrayList<>(List.of(
                    new RecipientGroup.FacilityRole(Role.DOCTOR), new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)));
            if (flags.contains("NO_MENU")) {
                recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
            }
            out.add(PortionNotices.request(
                    "PORTIONS_CLOSED_WITH_FLAGS",
                    dailyMealId,
                    key + ":FLAGS",
                    NotificationLevel.MEDIUM,
                    null,
                    recipients,
                    "Bữa ngày " + date + " đã chốt suất cần xử lý: " + String.join(", ", flags),
                    "Chốt suất cần xử lý",
                    by,
                    at));
        }
        if (residentsWithoutDiet > 0) {
            out.add(PortionNotices.request(
                    "RESIDENT_WITHOUT_DIET_ASSIGNMENT",
                    dailyMealId,
                    key + ":NO_DIET",
                    NotificationLevel.LOW,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                    residentsWithoutDiet + " người được tính suất bữa ngày " + date
                            + " chưa được gán chế độ ăn, đang dùng chế độ ăn mặc định",
                    "Chưa gán chế độ ăn",
                    by,
                    at));
        }
        if (safeDishApplied > 0) {
            out.add(PortionNotices.request(
                    "SAFE_DISH_APPLIED",
                    dailyMealId,
                    key + ":SAFE_DISH",
                    NotificationLevel.LOW,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                    safeDishApplied + " suất bữa ngày " + date + " đã tự dùng món an toàn, cần Bác sĩ xem lại",
                    "Đã dùng món an toàn",
                    by,
                    at));
        }
        for (long resident : safeDishConflicts) {
            out.add(PortionNotices.request(
                    "SAFE_DISH_CONFLICT",
                    dailyMealId,
                    key + ":SAFE_DISH_CONFLICT:" + resident,
                    NotificationLevel.MEDIUM,
                    resident,
                    List.of(new RecipientGroup.AssignedNurse(resident, new Substitution.Default())),
                    "Suất của người cao tuổi ở bữa ngày " + date
                            + " chưa có món phù hợp và món an toàn không dùng được; cần xử lý tại tầng",
                    "Suất chưa có món phù hợp",
                    by,
                    at));
        }
        return out;
    }
}
