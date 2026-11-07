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

/**
 * Món an toàn tự dùng sau chốt (FR-037a ii: tới mốc CFG-M08-05 mà Bác sĩ chưa xử lý; hoặc phát sinh ngoài giờ hành chính;
 * FR-065): Bác sĩ mức Nhẹ để xem lại; món an toàn cũng xung đột → điều dưỡng phụ trách người đó mức Trung bình. Bếp nhận qua
 * phát sinh ({@link AdjustmentsPending}).
 */
public record SafeDishApplied(
        long dailyMealId, LocalDate date, int applied, List<Long> conflicts, String sourceKey, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<NotificationRequest> out = new ArrayList<>();
        if (applied > 0) {
            out.add(PortionNotices.request(
                    "SAFE_DISH_APPLIED",
                    dailyMealId,
                    "M08.SAFE_DISH_APPLIED:" + dailyMealId + ":" + sourceKey,
                    NotificationLevel.LOW,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                    applied + " suất bữa ngày " + date + " đã tự dùng món an toàn, cần Bác sĩ xem lại",
                    "Đã dùng món an toàn",
                    null,
                    at));
        }
        for (long resident : conflicts) {
            out.add(PortionNotices.request(
                    "SAFE_DISH_CONFLICT",
                    dailyMealId,
                    "M08.SAFE_DISH_CONFLICT:" + dailyMealId + ":" + resident + ":" + sourceKey,
                    NotificationLevel.MEDIUM,
                    resident,
                    List.of(new RecipientGroup.AssignedNurse(resident, new Substitution.Default())),
                    "Suất của người cao tuổi ở bữa ngày " + date
                            + " chưa có món phù hợp và món an toàn không dùng được; cần xử lý tại tầng",
                    "Suất chưa có món phù hợp",
                    null,
                    at));
        }
        return out;
    }
}
