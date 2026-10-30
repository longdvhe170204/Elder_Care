package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
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
