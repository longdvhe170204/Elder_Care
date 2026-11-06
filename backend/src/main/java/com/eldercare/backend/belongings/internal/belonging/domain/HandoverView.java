package com.eldercare.backend.belongings.internal.belonging.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Một dòng lịch sử bàn giao trong chi tiết đồ gửi (FR-011, FR-012; SC-008). */
public record HandoverView(
        long id,
        long belongingId,
        String kind,
        String targetStatus,
        String giverType,
        String giverName,
        String recipientType,
        String recipientName,
        Instant occurredAt,
        BigDecimal quantity,
        BigDecimal cashAmount,
        Long conditionLevelId,
        String conditionDescription,
        Long storageLocationId,
        String reason,
        List<Long> photoFileIds,
        Long incidentId,
        Instant recordedAt) {}
