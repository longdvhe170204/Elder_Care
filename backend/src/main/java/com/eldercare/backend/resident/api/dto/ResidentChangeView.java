package com.eldercare.backend.resident.api.dto;

import java.time.Instant;
import java.util.Optional;

/** Một biến động trong khoảng thời gian: lệnh đổi trạng thái hoặc đổi mức chăm sóc (016 FR-023). */
public record ResidentChangeView(
        long residentId,
        Instant at,
        Optional<StatusCommand> command,
        Optional<ResidentStatus> fromStatus,
        Optional<ResidentStatus> toStatus,
        Optional<CareLevel> careLevelAfter) {}
