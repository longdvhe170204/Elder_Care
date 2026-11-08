package com.eldercare.backend.care.api.events;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Đính chính nguồn tính phí của hoạt động (014 FR-031, FR-031a; 010 FR-020): hủy nguồn ({@code cancelled}) khác số
 * lượng mới bằng 0. Cùng trường với {@code billing.api.events.CostSourceChanged}; khai ở {@code care} để tránh vòng
 * module như {@link ActivityCostSourceBilled}.
 */
public record ActivityCostSourceChanged(
        String sourceType,
        String sourceId,
        String unit,
        Optional<BigDecimal> newQuantity,
        boolean cancelled,
        String basis,
        String eventKey) {}
