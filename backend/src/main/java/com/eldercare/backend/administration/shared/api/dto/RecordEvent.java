package com.eldercare.backend.administration.shared.api.dto;

import java.time.Instant;

/** Một dòng của {@code record_event}. */
public record RecordEvent<T>(
        long id,
        EntityRef parent,
        T content,
        Long replyToId,
        Long actorId,
        boolean bySystem,
        Instant occurredAt,
        Instant deviceAt,
        Instant confirmedAt,
        Long floorAreaId) {}
