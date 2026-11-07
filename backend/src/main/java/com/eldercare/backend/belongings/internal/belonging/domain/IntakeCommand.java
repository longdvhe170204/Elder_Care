package com.eldercare.backend.belongings.internal.belonging.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Thân lệnh Tiếp nhận (api-013 {@code BelongingIntake}; FR-003, FR-005). */
public record IntakeCommand(
        long residentId,
        long typeId,
        String description,
        BigDecimal quantity,
        BigDecimal cashAmount,
        String unit,
        long conditionLevelId,
        String conditionDescription,
        GiverType giverType,
        Long giverId,
        String giverName,
        String giverIdDocument,
        Instant at,
        Long storageLocationId,
        List<Long> photoFileIds,
        String note,
        boolean giveForUseNow,
        Long consentSignedFormFileId) {

    public IntakeCommand {
        photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
    }
}
