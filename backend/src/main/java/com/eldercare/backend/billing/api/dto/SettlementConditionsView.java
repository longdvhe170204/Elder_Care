package com.eldercare.backend.billing.api.dto;

import java.util.List;

/**
 * Điều kiện quyết toán của người cao tuổi (tên logic {@code DieuKienQuyetToanView}). {@code currentStatementVersion}
 * rỗng khi chưa có bảng quyết toán.
 */
public record SettlementConditionsView(
        long residentId, List<SettlementConditionView> conditions, Integer currentStatementVersion) {}
