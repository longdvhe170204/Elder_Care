package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;

/** Trạng thái báo người nhà và các mốc tính hạn của một sự cố (BF-06). */
public record FamilyNoticeRow(
        Long residentId,
        IncidentLevel level,
        String status,
        Instant createdAt,
        boolean recordedLater,
        String state,
        Long notifiedBy,
        Instant notifiedAt,
        String method,
        String note) {}
