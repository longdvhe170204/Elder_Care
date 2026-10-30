package com.eldercare.backend.administration.shared.internal.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Một dòng {@code simple_record} như lưu; {@code content} là JSON chưa đọc theo loại. */
public record SimpleRow(
        long id,
        Long residentId,
        String entityType,
        Long entityId,
        String status,
        String content,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String uniqueKey,
        Instant lockedAt,
        Instant createdAt,
        long createdBy,
        int version) {}
