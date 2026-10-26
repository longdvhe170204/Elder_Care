package com.cambae.backend.m02.api.dto;

/**
 * Trường hợp kết thúc lưu trú (FR-062). {@code DEATH} chỉ xuất hiện ở {@link SettlementInfoView} cho người qua đời
 * (017 FR-034); hồ sơ kết thúc không bao giờ mang giá trị này.
 */
public enum EndCase {
    CONTRACT_EXPIRED,
    DISCHARGE,
    TRANSFER_TO_ANOTHER_FACILITY,
    CONTRACT_TERMINATION,
    DEATH;
}
