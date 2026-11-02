package com.eldercare.backend.health.api.events;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.time.Instant;

/** Đính chính dùng cùng phiên bản ngưỡng của bản gốc; không tự đóng cảnh báo (FR-019). */
public record VitalSignCorrected(
        long originalRecordId,
        long residentId,
        long indicatorId,
        VitalSignLevel originalLevel,
        VitalSignLevel currentLevel,
        String eventKey,
        Instant occurredAt) {}
