package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import java.time.Instant;

/** Bản gốc bất biến để kiểm đính chính và xác định tầng, người ghi gốc. */
public record AllocationCorrectionOriginal(
        long id,
        long residentId,
        long bedId,
        long floorId,
        String status,
        Long recorder,
        Instant startAt,
        Instant endAt,
        Instant expectedEnd) {}
