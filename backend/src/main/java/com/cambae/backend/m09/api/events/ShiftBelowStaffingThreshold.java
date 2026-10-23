package com.cambae.backend.m09.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record ShiftBelowStaffingThreshold(
        long shiftId,
        BigDecimal ratio,
        BigDecimal threshold,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ShiftBelowStaffingThreshold {
        notificationRequests = List.copyOf(notificationRequests);
    }

    public String eventKey() {
        return "SHIFT_BELOW_STAFFING_THRESHOLD:" + shiftId + ":" + occurredAt;
    }
}
