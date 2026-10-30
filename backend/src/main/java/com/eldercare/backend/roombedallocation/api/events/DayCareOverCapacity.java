package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import java.util.Comparator;
import java.util.List;

/** DayCareOverCapacity: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record DayCareOverCapacity(List<SessionDate> sessions, BedNotificationContext notification)
        implements HasNotificationRequests {
    public DayCareOverCapacity {
        sessions = List.copyOf(sessions);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "DAY_CARE_OVER_CAPACITY",
                0L,
                null,
                "DAY_CARE_REST_AREA",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                sessions.stream()
                        .distinct()
                        .sorted(Comparator.comparing(SessionDate::date).thenComparingLong(SessionDate::sessionId))
                        .toList()
                        .toString(),
                notification);
    }
}
