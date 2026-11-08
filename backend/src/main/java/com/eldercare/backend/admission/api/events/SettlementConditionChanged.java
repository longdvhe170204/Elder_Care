package com.eldercare.backend.admission.api.events;

/**
 * Điều kiện quyết toán của người cao tuổi đổi (tên logic {@code DieuKienQuyetToanDaDoi}; 017 FR-022): 017 phát, 004
 * đọc lại hai mã qua bean điều kiện chuyển trạng thái. Khai ở {@code admission.api} như {@link DepositStatusChanged}.
 */
public record SettlementConditionChanged(long residentId) {}
