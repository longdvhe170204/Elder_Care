package com.cambae.backend.m07.api.dto;

import java.time.LocalDate;

/**
 * Prescription view for family portal.
 */
public record FamilyPrescriptionView(
        long prescriptionId,
        long residentId,
        String medicationName,
        String dosageInstruction,
        String status,
        LocalDate startDate,
        LocalDate endDate) {}
