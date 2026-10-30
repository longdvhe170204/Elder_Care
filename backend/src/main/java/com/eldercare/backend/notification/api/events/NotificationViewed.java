package com.eldercare.backend.notification.api.events;

import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.notification.api.dto.RecipientType;
import java.time.Instant;

/** Người nhận xem thông báo lần đầu ({@code ThongBaoDaXem}; FR-034, FR-044; 012 BR-M10-06). */
public record NotificationViewed(
        long notificationId,
        SourceRecord sourceRecord,
        RecipientType recipientType,
        long recipientId,
        Instant viewedAt) {}
