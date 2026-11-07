package com.eldercare.backend.health.api.events;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.time.Instant;

/**
 * Đính chính dùng cùng phiên bản ngưỡng của bản gốc; không tự đóng cảnh báo (FR-019). {@code measuredAt} là thời điểm đo
 * của bản gốc, để quy tắc sụt cân xét lại lần cân đã đính chính (FR-039a, research S-10).
 */
public record VitalSignCorrected(
        long originalRecordId,
        long residentId,
        long indicatorId,
        VitalSignLevel originalLevel,
        VitalSignLevel currentLevel,
        Instant measuredAt,
        String eventKey,
        Instant occurredAt) {}
