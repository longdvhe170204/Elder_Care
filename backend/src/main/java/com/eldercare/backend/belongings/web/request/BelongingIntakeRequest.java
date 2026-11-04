package com.eldercare.backend.belongings.web.request;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.IntakeCommand;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Thân {@code BelongingIntake} (api-013; FR-003, FR-005). */
public record BelongingIntakeRequest(
        Long typeId,
        String description,
        BigDecimal quantity,
        BigDecimal cashAmount,
        String unit,
        ItemConditionRequest condition,
        GiverRequest giver,
        OffsetDateTime at,
        Long storageLocationId,
        List<Long> photoFileIds,
        String note,
        Boolean giveForUseNow,
        Long consentSignedFormFileId) {

    /** {@code ItemCondition}. */
    public record ItemConditionRequest(Long levelId, String description) {}

    /** {@code Giver}. */
    public record GiverRequest(GiverType type, Long id, String fullName, String idDocument) {}

    /** Thiếu trường bắt buộc của hợp đồng thì {@code null} (controller trả 422). */
    public IntakeCommand toCommand(long residentId) {
        if (typeId == null
                || description == null
                || description.isBlank()
                || condition == null
                || condition.levelId() == null
                || giver == null
                || at == null) {
            return null;
        }
        return new IntakeCommand(
                residentId,
                typeId,
                description,
                quantity,
                cashAmount,
                unit,
                condition.levelId(),
                condition.description(),
                giver.type(),
                giver.id(),
                giver.fullName(),
                giver.idDocument(),
                at.toInstant(),
                storageLocationId,
                photoFileIds,
                note,
                Boolean.TRUE.equals(giveForUseNow),
                consentSignedFormFileId);
    }
}
