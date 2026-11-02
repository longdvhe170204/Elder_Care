package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Lượt vắng của nội trú tới ngày thứ CFG-M02-05: báo Quản lý viện một lần, mức Trung bình (FR-055, BR-M02-07, Q-395). */
public record LongAbsenceManagerNotice(long absenceId, long residentId, int absentDays, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "LONG_ABSENCE_MANAGER_NOTICE",
                new SourceRecord("M02.ABSENCE", String.valueOf(absenceId)),
                "LONG_ABSENCE_MANAGER_NOTICE:" + absenceId,
                NotificationLevel.MEDIUM,
                residentId,
                Notices.MANAGER,
                "Lượt vắng dài ngày",
                "Một lượt vắng đã tới ngày thứ " + absentDays + ", cần xem xét giữ hay giải phóng giường.",
                at));
    }
}
