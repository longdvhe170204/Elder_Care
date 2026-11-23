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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Phiếu đối chiếu thuốc quá CFG-M07-10 (8 giờ) chưa được xác nhận: báo thêm Trưởng tầng và Quản lý viện (BR-M07-29,
 * Q-431). Mức Trung bình vì hệ thống chỉ có ba mức độ (9.3).
 */
public record ReconciliationEscalated(long ticketId, long residentId, Long floorAreaId, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> to = new ArrayList<>();
        if (floorAreaId != null) {
            to.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
        }
        to.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        return List.of(new NotificationRequest(
                "M07",
                "RECONCILIATION_ESCALATED",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.MEDICATION_RECONCILIATION", Long.toString(ticketId)),
                "RECONCILIATION_ESCALATED:" + ticketId,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.ofNullable(floorAreaId),
                to,
                List.of(new ContentPart(
                        InformationType.HEALTH, "Phiếu đối chiếu thuốc quá hạn lần hai, chưa được xác nhận")),
                Optional.of("Đối chiếu thuốc quá hạn lần hai"),
                Optional.of("Đối chiếu thuốc quá hạn lần hai"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("ticketId", ticketId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
