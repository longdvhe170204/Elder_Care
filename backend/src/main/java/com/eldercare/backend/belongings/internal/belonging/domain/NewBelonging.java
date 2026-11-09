package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import java.math.BigDecimal;
import java.time.Instant;

/** Đồ gửi mới tạo bởi lệnh Tiếp nhận (FR-004); cột suy ra đặt theo bản ghi Tiếp nhận. */
public record NewBelonging(
        long residentId,
        long typeId,
        String description,
        BigDecimal quantity,
        BigDecimal cashAmount,
        String unit,
        BelongingStatus status,
        HolderType holderType,
        Long holderId,
        Long storageLocationId,
        long conditionLevelId,
        long createdBy,
        Instant createdAt) {}
