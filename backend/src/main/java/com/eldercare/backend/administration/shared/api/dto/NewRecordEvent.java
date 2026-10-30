package com.eldercare.backend.administration.shared.api.dto;

import java.time.Instant;

/** Bản ghi con mới của {@code record_event}; {@code confirmedAt} {@code null} là bản nháp (mục 1.4). */
public record NewRecordEvent<T>(
        T content,
        Long replyToId,
        Long actorId,
        boolean bySystem,
        Instant occurredAt,
        Instant deviceAt,
        Instant confirmedAt,
        Long floorAreaId,
        String uniqueKey) {}
