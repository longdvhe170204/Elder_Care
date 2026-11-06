package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một bản ghi bàn giao với các cột dùng để tính lại đồ (research K-05): sau đính chính là giá trị hiện hành
 * ({@link #amended}).
 */
public record HandoverRow(
        long id,
        long belongingId,
        long residentId,
        HandoverType kind,
        BelongingStatus targetStatus,
        GiverType recipientType,
        Long recipientId,
        BigDecimal quantity,
        BigDecimal cashAmount,
        Long conditionLevelId,
        Long storageLocationId,
        Long incidentId,
        long recordedBy,
        Instant occurredAt,
        Instant recordedAt) {

    /** Số lượng, hoặc số tiền với tiền mặt. */
    public BigDecimal amount() {
        return quantity != null ? quantity : cashAmount;
    }

    public HandoverRow amended(
            BigDecimal quantity,
            BigDecimal cashAmount,
            Long conditionLevelId,
            Long storageLocationId,
            Long recipientId) {
        return new HandoverRow(
                id,
                belongingId,
                residentId,
                kind,
                targetStatus,
                recipientType,
                recipientId,
                quantity,
                cashAmount,
                conditionLevelId,
                storageLocationId,
                incidentId,
                recordedBy,
                occurredAt,
                recordedAt);
    }
}
