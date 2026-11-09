package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import java.math.BigDecimal;

/** Một đồ của một lần tiếp nhận (gồm đồ tách, đồ bị loại) đã khóa dòng để tính lại sau lệnh, đính chính (K-04, K-05). */
public record IntakeGroupMember(
        long id,
        Long originalBelongingId,
        BigDecimal quantity,
        BigDecimal cashAmount,
        boolean excluded,
        BelongingStatus status,
        HolderType holderType,
        Long holderId,
        Long storageLocationId,
        long conditionLevelId,
        Long currentHandoverId) {

    public BigDecimal amount() {
        return quantity != null ? quantity : cashAmount;
    }
}
