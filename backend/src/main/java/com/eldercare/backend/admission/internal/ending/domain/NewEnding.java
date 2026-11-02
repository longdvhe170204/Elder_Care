package com.eldercare.backend.admission.internal.ending.domain;

import com.eldercare.backend.admission.api.dto.EndCase;
import java.time.Instant;
import java.time.LocalDate;

/** Dữ liệu lập hồ sơ kết thúc. */
public record NewEnding(
        long residentId,
        EndCase endCase,
        boolean medicalReason,
        String destination,
        LocalDate expectedEndDate,
        String requestedBy,
        String reason,
        long createdBy,
        Instant createdAt) {}
