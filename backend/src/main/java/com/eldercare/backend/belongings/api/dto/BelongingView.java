package com.eldercare.backend.belongings.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một đồ gửi với cột suy ra từ bản ghi bàn giao hiện hành (SC-008). Đúng một trong {@code quantity}, {@code cashAmount}
 * có giá trị theo loại đồ.
 */
public record BelongingView(
        long belongingId,
        long residentId,
        long typeId,
        String description,
        BigDecimal quantity,
        BigDecimal cashAmount,
        String unit,
        BelongingStatus status,
        HolderType holderType,
        Long holderId,
        Long storageLocationId,
        Long originalBelongingId,
        Instant lastHandoverAt,
        int version) {}
