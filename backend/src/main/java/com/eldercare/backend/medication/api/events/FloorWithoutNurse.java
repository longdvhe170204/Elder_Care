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

/** Tầng không có Điều dưỡng trong ca nhưng có liều thuốc sắp tới (BR-M07-21). */
public record FloorWithoutNurse(long floorAreaId, long shiftId, int upcomingDoseCount, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M07",
                "FLOOR_WITHOUT_NURSE",
                SourceGroup.MEDICATION,
                new SourceRecord("M09.SHIFT", Long.toString(shiftId)),
                "FLOOR_WITHOUT_NURSE:" + floorAreaId + ":" + shiftId,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.of(floorAreaId),
                List.of(
                        new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                List.of(new ContentPart(
                        InformationType.GENERAL,
                        "Tầng không có Điều dưỡng trong ca; có " + upcomingDoseCount + " liều thuốc sắp tới")),
                Optional.of("Tầng không có Điều dưỡng trong ca"),
                Optional.of("Tầng không có Điều dưỡng trong ca"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("shiftId", shiftId, "upcomingDoseCount", upcomingDoseCount),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
