package com.cambae.backend.m07.api.dto;

import java.time.LocalDate;

/**
 * Thông tin thuốc gia đình gửi (ThuocGuiView).
 */
public record FamilyMedicationView(
        long batchId,
        long residentId,
        String medicationName,
        String status,
        int remainingQuantity,
        String unit,
        LocalDate expiryDate) {}
