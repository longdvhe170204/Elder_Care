package com.eldercare.backend.incident.internal.incident.domain;

import java.time.Instant;

/**
 * Tình trạng báo người nhà của một sự cố (BF-06). {@code state}: NONE (chưa ghi gì), INFORMED (đã báo), NOT_YET (ghi nhận
 * sau, chưa báo). {@code dueAt}: hạn báo theo mức; {@code overdue}: sự cố còn mở, chưa ghi đã báo và quá hạn.
 */
public record FamilyNoticeStatus(
        boolean recordedLater,
        String state,
        Instant notifiedAt,
        String method,
        String note,
        Long notifiedById,
        String notifiedByName,
        Instant dueAt,
        boolean overdue) {}
