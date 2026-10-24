package com.cambae.backend.m01.api.dto;

import java.util.Optional;

/** Trạng thái và mức chăm sóc hiệu lực của một người cao tuổi tại ngày (016 FR-022). */
public record ResidentStatusAtDateView(long residentId, ResidentStatus status, Optional<CareLevel> careLevel) {}
