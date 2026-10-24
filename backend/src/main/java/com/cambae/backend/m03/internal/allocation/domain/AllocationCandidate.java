package com.cambae.backend.m03.internal.allocation.domain;

import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import java.util.Set;

/** Ảnh đọc một giường và các chính sách liên quan để kiểm FR-018. */
public record AllocationCandidate(
        long bedId,
        long roomId,
        long floorId,
        long areaId,
        BedStatus state,
        java.time.Instant readyDeadline,
        Long heldForResidentId,
        boolean active,
        boolean isolated,
        GenderPolicy genderPolicy,
        Set<CareLevelCode> careLevels,
        boolean servesLongTerm,
        boolean servesShortTerm) {}
