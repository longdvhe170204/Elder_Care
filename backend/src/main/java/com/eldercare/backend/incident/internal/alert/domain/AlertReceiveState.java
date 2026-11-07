package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;

/** Phần cảnh báo đã khóa mà lệnh Tiếp nhận cần (bảng FR-029). */
public record AlertReceiveState(
        long id,
        Long residentId,
        Long floorAreaId,
        AlertStatus status,
        int escalationLevel,
        IncidentLevel level,
        Long receiverId,
        Instant receivedAt,
        int version) {}
