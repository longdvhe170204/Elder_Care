package com.eldercare.backend.administration.shared.api.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Một dòng của {@code simple_record}; {@code lockedAt} khác {@code null} là đã khóa. */
public record SimpleRecord<T>(
        long id,
        Long residentId,
        EntityRef parent,
        String status,
        T content,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String uniqueKey,
        Instant lockedAt,
        Instant createdAt,
        long createdBy,
        int version) {}
