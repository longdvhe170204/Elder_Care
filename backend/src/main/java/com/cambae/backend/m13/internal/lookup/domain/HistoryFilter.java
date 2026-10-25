package com.cambae.backend.m13.internal.lookup.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/** Bộ lọc của {@code GET /notification-history}; {@code floors} rỗng là toàn viện. */
public record HistoryFilter(
        String sourceRecordType,
        String sourceRecordId,
        Long recipientId,
        Long floorAreaId,
        Instant from,
        Instant to,
        Optional<Collection<Long>> floors) {}
