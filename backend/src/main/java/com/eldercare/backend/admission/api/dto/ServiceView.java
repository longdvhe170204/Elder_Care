package com.eldercare.backend.admission.api.dto;

/**
 * Dịch vụ trong danh mục (FR-034); {@code unit} là đơn vị tính hiển thị; {@code active} là trạng thái Hiệu lực; {@code
 * medicationId} chỉ có với thuốc nguồn viện.
 */
public record ServiceView(
        long id,
        ServiceType type,
        String name,
        String unit,
        BillingPeriod billingPeriod,
        boolean active,
        Long medicationId) {}
