package com.eldercare.backend.notification.internal.lookup.domain;

import java.time.Instant;

/** Một dòng lỗi gửi (FR-038). */
public record DeliveryError(
        String type,
        long requestId,
        Long notificationId,
        String eventType,
        String sourceRecordType,
        String sourceRecordId,
        String eventKey,
        String reason,
        Instant receivedAt) {}
