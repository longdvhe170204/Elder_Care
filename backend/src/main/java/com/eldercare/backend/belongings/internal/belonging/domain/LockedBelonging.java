package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import java.math.BigDecimal;

/** Đồ gửi đã khóa dòng trong lệnh (research K-03): cột suy ra hiện hành và phiên bản. */
public record LockedBelonging(
        long id,
        long residentId,
        long typeId,
        BelongingStatus status,
        HolderType holderType,
        Long holderId,
        Long storageLocationId,
        long conditionLevelId,
        BigDecimal quantity,
        BigDecimal cashAmount,
        int version) {}
