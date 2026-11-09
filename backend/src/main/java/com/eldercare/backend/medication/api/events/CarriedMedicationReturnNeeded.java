package com.eldercare.backend.medication.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chuyến bị hủy, dời hoặc người cao tuổi "Không đi" sau khi đã giao thuốc mang theo: nhắc Điều dưỡng phụ trách ghi
 * Nhận lại (FR-048b, Q-58; mức Trung bình theo 014 FR-071).
 */
public record CarriedMedicationReturnNeeded(long handoverId, long residentId, long tripId, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M07",
                "CARRIED_MEDICATION_RETURN_NEEDED",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.MEDICATION_HANDOVER", Long.toString(handoverId)),
                "CARRIED_MEDICATION_RETURN_NEEDED:" + handoverId + ":" + occurredAt.toEpochMilli(),
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default())),
                List.of(new ContentPart(
                        InformationType.HEALTH,
                        "Chuyến đi đã thay đổi sau khi giao thuốc mang theo; cần ghi Nhận lại")),
                Optional.of("Cần nhận lại thuốc mang theo"),
                Optional.of("Cần nhận lại thuốc mang theo"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("handoverId", handoverId, "tripId", tripId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
