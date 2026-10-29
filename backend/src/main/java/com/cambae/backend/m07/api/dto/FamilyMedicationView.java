package com.cambae.backend.m07.api.dto;

import java.time.LocalDate;

/**
 * Family-sent medication view; số lượng lúc tiếp nhận, không có số lượng còn (Q-393).
 */
public record FamilyMedicationView(
        long batchId,
        long residentId,
        String medicationName,
        String status,
        int initialQuantity,
        String unit,
        LocalDate expiryDate) {}
