package com.eldercare.backend.resident.api.dto;

import java.time.Instant;

/** Mức chăm sóc hiệu lực tại một ngày, kèm căn cứ (FR-053). */
public record CareLevelAtDate(CareLevel careLevel, Instant effectiveFrom, CareLevelBasisType basisType, long basisId) {}
