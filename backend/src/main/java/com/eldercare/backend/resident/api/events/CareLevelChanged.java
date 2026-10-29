package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareLevelBasisType;
import java.time.Instant;
import java.util.Optional;

/** Mức chăm sóc hiệu lực đổi; 003, 008, 010, 016 nghe (FR-052). */
public record CareLevelChanged(
        long residentId,
        Optional<CareLevel> previous,
        CareLevel current,
        Instant effectiveFrom,
        CareLevelBasisType basisType,
        long basisId) {}
