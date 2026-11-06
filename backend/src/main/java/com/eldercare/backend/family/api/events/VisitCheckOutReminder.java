package com.eldercare.backend.family.api.events;

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

/** Lượt Đã vào quá giờ kết thúc khung cộng CFG-M10-07 chưa ghi giờ ra: nhắc Hành chính một lần (FR-034). */
public record VisitCheckOutReminder(long visitId, long residentId, Instant at) implements HasNotificationRequests {

    private static final String TITLE = "Lượt thăm chưa ghi giờ ra";

    @Override
    public List<NotificationRequest> notificationRequests() {
        String summary = TITLE + ": khung thăm đã kết thúc, ghi giờ ra thực tế của khách";
        return List.of(new NotificationRequest(
                "M10",
                "VISIT_CHECK_OUT_REMINDER",
                SourceGroup.FAMILY_INFO,
                new SourceRecord(VisitCancelledNotice.SOURCE_TYPE, String.valueOf(visitId)),
                VisitCancelledNotice.SOURCE_TYPE + ":" + visitId + ":CHECK_OUT_REMINDER",
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(TITLE),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
