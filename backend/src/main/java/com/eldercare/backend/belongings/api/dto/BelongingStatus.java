package com.eldercare.backend.belongings.api.dto;

/** Trạng thái đồ gửi (bảng trạng thái của spec 013; data-model mục 3). */
public enum BelongingStatus {
    HELD,
    IN_USE,
    RETURNED,
    LOST,
    DAMAGED,
    DISPOSED
}
