package com.eldercare.backend.admission.internal.absence.domain;

import java.time.Instant;

/** Dữ liệu mở một lượt vắng. */
public record NewAbsence(
        long residentId,
        AbsenceType type,
        String reason,
        String standardReason,
        Instant departedAt,
        Instant expectedReturnAt,
        Long pickedUpById,
        Long pickupRecordId,
        String transferUnit,
        long handoverStaffId,
        boolean takesMedication,
        Long previousAbsenceId,
        long createdBy,
        Instant createdAt) {}
