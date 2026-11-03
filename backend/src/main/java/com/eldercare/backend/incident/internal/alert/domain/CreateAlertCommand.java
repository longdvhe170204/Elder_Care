package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;

/** Lệnh yêu cầu tạo hoặc gộp cảnh báo từ các nguồn nghiệp vụ (FR-027, FR-028, S-08). */
public record CreateAlertCommand(
        Long residentId,
        Long floorAreaId,
        String type,
        String typeLock,
        IncidentLevel level,
        Instant occurredAt,
        String sourceType,
        Long sourceId,
        String description,
        Long createdById) {}
