package com.eldercare.backend.health.api.events;

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

/** Nhắc thiếu ngưỡng cá nhân mức Nhẹ cho Bác sĩ toàn viện (FR-025). */
public record MissingPersonalThresholdReminder(long residentId, String eventKey, Instant occurredAt)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M06",
                "MISSING_PERSONAL_THRESHOLD",
                SourceGroup.OTHER,
                new SourceRecord("M06.RESIDENT_VITAL_SIGNS", Long.toString(residentId)),
                eventKey,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                List.of(new ContentPart(InformationType.HEALTH, "Người cao tuổi chưa có ngưỡng chỉ số cá nhân")),
                Optional.of("Có thông tin cần xem trong ứng dụng"),
                Optional.of("Nhắc thiết lập ngưỡng cá nhân"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("residentId", residentId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
