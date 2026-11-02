package com.eldercare.backend.administration.shared.api.dto;

import com.eldercare.backend.common.Reason;
import java.time.Instant;

/** Dòng lịch sử mới; {@code uniqueKey} thay ràng buộc duy nhất riêng của bảng lịch sử cũ (research R-15). */
public record HistoryEntry<D>(
        HistoryKind kind,
        String valueBefore,
        String value,
        Long valueId,
        Instant startsAt,
        String command,
        Long actorId,
        boolean bySystem,
        Reason reason,
        String basisType,
        Long basisId,
        D details,
        Long floorAreaId,
        String uniqueKey) {}
