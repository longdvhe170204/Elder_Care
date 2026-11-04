package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Một bản ghi bàn giao mới (data-model mục 4); người giao, người nhận dùng cùng kiểu {@link GiverType}. */
public record NewHandover(
        long belongingId,
        long residentId,
        HandoverType kind,
        BelongingStatus targetStatus,
        GiverType giverType,
        Long giverId,
        String giverName,
        String giverIdDocument,
        GiverType recipientType,
        Long recipientId,
        String recipientName,
        Instant occurredAt,
        BigDecimal quantity,
        BigDecimal cashAmount,
        Long conditionLevelId,
        String conditionDescription,
        Long storageLocationId,
        String reason,
        boolean holderAbsent,
        boolean valuable,
        List<Long> photoFileIds,
        String note,
        long recordedBy,
        Instant recordedAt) {

    public NewHandover {
        photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
    }
}
