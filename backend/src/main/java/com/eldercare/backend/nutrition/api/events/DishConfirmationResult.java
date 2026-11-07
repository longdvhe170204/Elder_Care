package com.eldercare.backend.nutrition.api.events;

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
 * Món hoặc bản sửa được xác nhận hoặc bị trả lại (FR-065 dòng "Món hoặc bản sửa được xác nhận, bị trả lại"): báo Nhân
 * viên bếp đã gửi, mức Nhẹ, kèm lý do khi trả lại. {@code submitterStaffId} là hồ sơ nhân viên của người gửi.
 */
public record DishConfirmationResult(
        long dishId,
        String dishName,
        long versionId,
        boolean revision,
        boolean confirmed,
        String reason,
        long submitterStaffId,
        long decidedBy,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String what = (revision ? "Bản sửa món " : "Món ") + "\"" + dishName + "\"";
        String text = confirmed ? what + " đã được Bác sĩ xác nhận" : what + " bị trả lại: " + reason;
        return List.of(new NotificationRequest(
                "M08",
                confirmed ? "DISH_CONFIRMED" : "DISH_RETURNED",
                SourceGroup.OTHER,
                new SourceRecord("M08.DISH", String.valueOf(dishId)),
                (confirmed ? "M08.DISH_CONFIRMED:" : "M08.DISH_RETURNED:") + versionId,
                Optional.of(NotificationLevel.LOW),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.Staff(submitterStaffId, "DISH_SUBMITTER")),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.empty(),
                Optional.of(confirmed ? "Món đã được xác nhận" : "Món bị trả lại"),
                Optional.of(decidedBy),
                false,
                at,
                Map.of("dishId", dishId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
