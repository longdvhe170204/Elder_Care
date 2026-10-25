package com.cambae.backend.m07.api.dto;

import java.time.LocalDate;

/**
 * Family-sent medication view.
 */
public record FamilyMedicationView(
        long batchId,
        long residentId,
        String medicationName,
        String status,
        int remainingQuantity,
        String unit,
        LocalDate expiryDate) {}
