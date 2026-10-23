package com.cambae.backend.m07.api.spi;

import java.math.BigDecimal;

/**
 * Dòng yêu cầu thuốc đã mua từ module M11/019 (DongYeuCauThuocView).
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
