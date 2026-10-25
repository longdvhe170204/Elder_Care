package com.cambae.backend.m07.api.spi;

import java.math.BigDecimal;

/**
 * Purchased medication request line view from module M11/019.
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
