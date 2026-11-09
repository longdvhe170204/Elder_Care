package com.eldercare.backend.medication.api.events;

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
 * "Thuốc đang sử dụng khi tiếp nhận" đổi sau khi lập phiếu đối chiếu lý do tiếp nhận (FR-034a, BR-M07-22): báo người
 * thực hiện (phiếu Mở), hoặc người thực hiện và người xác nhận (Chờ xác nhận, Đã xác nhận). Phiếu không đổi.
 */
public record AdmissionMedicationChanged(
        long reconciliationId, long residentId, List<Long> staffIds, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M07",
                "ADMISSION_MEDICATION_CHANGED",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.RECONCILIATION", Long.toString(reconciliationId)),
                "ADMISSION_MEDICATION_CHANGED:" + reconciliationId + ":" + occurredAt.toEpochMilli(),
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                staffIds.stream()
                        .<RecipientGroup>map(id -> new RecipientGroup.Staff(id, "RECONCILIATION_PARTICIPANT"))
                        .toList(),
                List.of(new ContentPart(
                        InformationType.HEALTH,
                        "Thuốc đang sử dụng khi tiếp nhận đã đổi sau khi lập phiếu đối chiếu; cần xem lại phiếu")),
                Optional.of("Thuốc khi tiếp nhận đã đổi"),
                Optional.of("Thuốc khi tiếp nhận đã đổi"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("reconciliationId", reconciliationId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
