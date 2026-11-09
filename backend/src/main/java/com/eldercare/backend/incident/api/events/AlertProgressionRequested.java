package com.eldercare.backend.incident.api.events;

import java.time.Instant;

/**
 * Yêu cầu ghi diễn biến vào cảnh báo đang mở của nguồn (007 FR-086, mục 1.13; 014 FR-047, FR-056). {@code actorId} là
 * người thực hiện lệnh nguồn, {@code occurredAt} thời điểm của lệnh ("Đổi api" chủ dự án duyệt 2026-11-07).
 */
public record AlertProgressionRequested(
        String sourceType,
        long sourceId,
        String progressionTypeCode,
        String content,
        String eventKey,
        Long actorId,
        Instant occurredAt) {}
