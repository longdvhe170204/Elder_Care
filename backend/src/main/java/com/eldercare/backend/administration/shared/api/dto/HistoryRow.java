package com.eldercare.backend.administration.shared.api.dto;

import java.time.Instant;

/** Một dòng của {@code status_history}. */
public record HistoryRow<D>(
        long id,
        HistoryKind kind,
        String valueBefore,
        String value,
        Long valueId,
        Instant startsAt,
        Instant endsAt,
        String command,
        Long actorId,
        boolean bySystem,
        String reason,
        String basisType,
        Long basisId,
        D details,
        Long floorAreaId) {}
