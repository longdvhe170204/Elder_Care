package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m01.api.dto.CareLevel;
import java.time.Instant;
import java.util.List;

/** Nhắc lập yêu cầu đổi mức; báo Hành chính mức Nhẹ. Khóa sự kiện (residentId, assessmentId, reminderNumber). */
public record CareLevelChangeRequestReminder(
        long residentId, long assessmentId, CareLevel newLevel, int reminderNumber, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "CARE_LEVEL_CHANGE_REQUEST_REMINDER",
                NotificationLevel.LOW,
                new SourceRecord("M01.ASSESSMENT", String.valueOf(assessmentId)),
                "CARE_LEVEL_CHANGE_REQUEST_REMINDER:" + residentId + ":" + assessmentId + ":" + reminderNumber,
                residentId,
                null,
                List.of(Notices.OFFICE),
                Notices.general(
                        "Nhắc lần " + reminderNumber + ": còn thiếu yêu cầu đổi mức chăm sóc (" + newLevel + ")."),
                "Nhắc lập yêu cầu đổi mức chăm sóc",
                "Nhắc lập yêu cầu đổi mức chăm sóc cho người cao tuổi.",
                at));
    }
}
