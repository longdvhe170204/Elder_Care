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
 * Báo Bác sĩ xem lại chế độ ăn (FR-058, FR-065, BR-M08-05, Q-307): ăn kém kéo dài (005) hoặc sụt cân (007), mức Nhẹ, kèm nguồn
 * và chế độ ăn hiện hành. Không phải yêu cầu: không hạn xử lý, không nhắc lại. Tiêu đề không chứa thông tin sức khỏe; nguồn và
 * chế độ ăn ở phần "sức khỏe".
 */
public record ReviewDietNotice(long noticeId, long residentId, Source source, String currentDiet, Instant at)
        implements HasNotificationRequests {

    /** Nguồn của thông báo (data-model mục 10 {@code nguon}). */
    public enum Source {
        PROLONGED_POOR_EATING,
        WEIGHT_LOSS
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        String what = source == Source.PROLONGED_POOR_EATING ? "ăn kém kéo dài" : "sụt cân";
        return List.of(new NotificationRequest(
                "M08",
                "DIET_REVIEW",
                SourceGroup.OTHER,
                new SourceRecord("M08.REVIEW_NOTICE", String.valueOf(noticeId)),
                "M08.DIET_REVIEW:" + noticeId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                List.of(
                        new ContentPart(InformationType.GENERAL, "Cần xem lại chế độ ăn của một người cao tuổi"),
                        new ContentPart(
                                InformationType.HEALTH, "Nguồn: " + what + ". Chế độ ăn hiện hành: " + currentDiet)),
                Optional.empty(),
                Optional.of("Xem lại chế độ ăn"),
                Optional.empty(),
                false,
                at,
                Map.of("noticeId", noticeId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
