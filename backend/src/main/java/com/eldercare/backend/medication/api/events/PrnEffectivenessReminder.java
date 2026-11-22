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

/** Sau CFG-M07-11 (60 phút) kể từ lần dùng thuốc khi cần mà chưa ghi hiệu quả: nhắc Điều dưỡng (mức Nhẹ; BR-M07-04). */
public record PrnEffectivenessReminder(long prnAdministrationId, long residentId, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M07",
                "PRN_EFFECTIVENESS_REMINDER",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.PRN_ADMINISTRATION", Long.toString(prnAdministrationId)),
                "PRN_EFFECTIVENESS_REMINDER:" + prnAdministrationId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default())),
                List.of(new ContentPart(InformationType.HEALTH, "Cần ghi hiệu quả sau lần dùng thuốc khi cần")),
                Optional.of("Ghi hiệu quả thuốc khi cần"),
                Optional.of("Ghi hiệu quả thuốc khi cần"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("prnAdministrationId", prnAdministrationId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
