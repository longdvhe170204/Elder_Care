package com.eldercare.backend.roombedallocation.internal.vehicle.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** 019 FR-018, FR-021, Q-236, Q-298: thông báo của lịch xe (dời, hủy, gia hạn chồng lịch, quá giờ trả xe). */
public record VehicleScheduleNotice(long scheduleId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public VehicleScheduleNotice {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
