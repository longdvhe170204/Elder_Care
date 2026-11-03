package com.eldercare.backend.care.internal.result.domain;

import java.time.Instant;
import java.util.Map;

/** T035: bản gốc bất biến, id trùng id task theo R-15. */
public record CareResult(
        long id,
        Long residentId,
        String resultType,
        Map<String, Object> values,
        Instant performedAt,
        Instant recordedAt,
        Instant deviceAt,
        long recordedBy,
        boolean lateRecording,
        boolean permissionLost,
        String permissionBasis,
        Long vitalSignRecordId) {}
