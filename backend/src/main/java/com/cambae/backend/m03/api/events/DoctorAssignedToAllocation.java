package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
import java.util.List;

/** DoctorAssignedToAllocation: mức LOW, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record DoctorAssignedToAllocation(long allocationId, long doctorId, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "DOCTOR_ASSIGNED_TO_ALLOCATION",
                allocationId,
                null,
                "BED_ALLOCATION",
                NotificationLevel.LOW,
                InformationType.GENERAL,
                "",
                notification);
    }
}
