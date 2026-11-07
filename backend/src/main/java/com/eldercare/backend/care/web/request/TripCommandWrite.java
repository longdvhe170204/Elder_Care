package com.eldercare.backend.care.web.request;

import java.time.Instant;
import java.util.List;

/** Phần thân TripCommand dùng ở Phase 6; các trường còn lại giữ để nhận đúng hợp đồng chung. */
public record TripCommandWrite(
        List<Long> residentIds,
        String reason,
        Instant newPlannedReturnAt,
        Long newLeaderId,
        String lastSeenPlace,
        Instant lastSeenAt,
        String description,
        Instant deviceAt) {}
