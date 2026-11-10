package com.eldercare.backend.billing.api.dto;

import java.math.BigDecimal;

/**
 * Dòng thuốc đã ghi đã mua (019 module-interfaces mục cuối, cùng trường với {@code ProcurementMedicationLineView} 006
 * khai trước): {@code creatorStaffId} là người lập, {@code departmentId} là bộ phận của yêu cầu (không đổi khi người lập
 * chuyển bộ phận; 006 báo Phụ trách bộ phận hiện tại của bộ phận này).
 */
public record ProcurementMedicationLineView(
        long procurementLineId,
        long procurementRequestId,
        long residentId,
        long prescriptionId,
        String medicationName,
        String activeIngredient,
        String strength,
        String dosageForm,
        BigDecimal quantity,
        String unit,
        long creatorStaffId,
        long departmentId) {}
