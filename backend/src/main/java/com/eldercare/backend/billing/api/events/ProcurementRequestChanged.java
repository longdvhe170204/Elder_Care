package com.eldercare.backend.billing.api.events;

import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.time.Instant;
import java.util.List;

/** 019 contracts mục 4 (YeuCauMuaSamDaDoi) cho 016; thông báo của lệnh (FR-021) đi kèm. from rỗng khi lập. */
public record ProcurementRequestChanged(
        long requestId,
        long departmentId,
        ProcurementType type,
        ProcurementStatus from,
        ProcurementStatus to,
        String command,
        Instant at,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ProcurementRequestChanged {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
