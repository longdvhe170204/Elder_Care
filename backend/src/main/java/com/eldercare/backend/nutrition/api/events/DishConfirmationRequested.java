package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Món hoặc bản sửa món được gửi xác nhận (FR-065 dòng "Món hoặc bản sửa món được gửi xác nhận"): báo Bác sĩ, mức Nhẹ.
 * Chỉ tới nhân viên, không có thông tin của người cao tuổi. Mỗi phiên bản nội dung một khóa sự kiện.
 */
public record DishConfirmationRequested(
        long dishId, String dishName, long versionId, boolean revision, long submittedBy, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String what = revision ? "Bản sửa món " : "Món ";
        return List.of(new NotificationRequest(
                "M08",
                "DISH_CONFIRMATION_REQUESTED",
                SourceGroup.OTHER,
                new SourceRecord("M08.DISH", String.valueOf(dishId)),
                "M08.DISH_CONFIRMATION_REQUESTED:" + versionId,
                Optional.of(NotificationLevel.LOW),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                List.of(new ContentPart(InformationType.GENERAL, what + "\"" + dishName + "\" chờ Bác sĩ xác nhận")),
                Optional.empty(),
                Optional.of("Món chờ xác nhận"),
                Optional.of(submittedBy),
                false,
                at,
                Map.of("dishId", dishId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
