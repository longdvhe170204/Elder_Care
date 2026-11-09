package com.eldercare.backend.medication.api.events;

import com.eldercare.backend.common.Role;
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
 * Hoàn tất tiếp nhận hoặc Ghi nhận trở về từ bệnh viện đã tạm dừng đơn chờ đối chiếu: báo Điều dưỡng phụ trách và các
 * Bác sĩ lập phiếu đối chiếu (FR-034, BR-M07-09; mức Trung bình theo 009 FR-043b).
 */
public record ReconciliationNeeded(long residentId, String reason, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M07",
                "RECONCILIATION_NEEDED",
                SourceGroup.MEDICATION,
                new SourceRecord("M01.RESIDENT", Long.toString(residentId)),
                "RECONCILIATION_NEEDED:" + residentId + ":" + reason + ":" + occurredAt.toEpochMilli(),
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.empty(),
                List.of(
                        new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.DOCTOR)),
                List.of(new ContentPart(
                        InformationType.HEALTH, "Đơn thuốc đã tạm dừng chờ đối chiếu; cần lập phiếu đối chiếu thuốc")),
                Optional.of("Cần lập phiếu đối chiếu thuốc"),
                Optional.of("Cần lập phiếu đối chiếu thuốc"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("reason", reason),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
