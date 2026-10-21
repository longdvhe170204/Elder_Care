package com.cambae.backend.m13.api.events;

import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m13.api.dto.RecipientType;
import java.time.Instant;

/** Người nhận xem thông báo lần đầu ({@code ThongBaoDaXem}; FR-034, FR-044; 012 BR-M10-06). */
public record NotificationViewed(
        long notificationId,
        SourceRecord sourceRecord,
        RecipientType recipientType,
        long recipientId,
        Instant viewedAt) {}
