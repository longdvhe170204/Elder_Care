package com.eldercare.backend.administration.shared.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import java.time.Instant;

/** Một dòng {@code status_history} như lưu; {@code details} là JSON chưa đọc theo loại. */
public record HistoryRecord(
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
        String details,
        Long floorAreaId) {}
