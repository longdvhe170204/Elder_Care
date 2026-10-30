package com.eldercare.backend.administration.shared.internal.domain;

import java.time.Instant;

/** Một dòng {@code record_event} như lưu; {@code content} là JSON chưa đọc theo loại. */
public record EventRow(
        long id,
        String entityType,
        long entityId,
        String content,
        Long replyToId,
        Long actorId,
        boolean bySystem,
        Instant occurredAt,
        Instant deviceAt,
        Instant confirmedAt,
        Long floorAreaId) {}
