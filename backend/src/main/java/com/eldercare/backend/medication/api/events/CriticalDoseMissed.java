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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Liều Bỏ lỡ của đơn Quan trọng về thời gian: báo ngay Điều dưỡng phụ trách, Trưởng tầng và Bác sĩ trực, không chờ leo
 * thang (BR-M07-27, Q-428). Mức Trung bình vì hệ thống chỉ có ba mức độ (9.3). {@code floorAreaId} trống thì không có
 * Trưởng tầng.
 */
public record CriticalDoseMissed(long doseId, long residentId, Long floorAreaId, Instant occurredAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> to = new ArrayList<>();
        to.add(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
        if (floorAreaId != null) {
            to.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
        }
        to.add(new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
        return List.of(new NotificationRequest(
                "M07",
                "CRITICAL_DOSE_MISSED",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.MEDICATION_DOSE", Long.toString(doseId)),
                "CRITICAL_DOSE_MISSED:" + doseId,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.ofNullable(floorAreaId),
                to,
                List.of(new ContentPart(InformationType.HEALTH, "Liều thuốc quan trọng về thời gian đã bị bỏ lỡ")),
                Optional.of("Liều thuốc quan trọng bị bỏ lỡ"),
                Optional.of("Liều thuốc quan trọng bị bỏ lỡ"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of("doseId", doseId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
