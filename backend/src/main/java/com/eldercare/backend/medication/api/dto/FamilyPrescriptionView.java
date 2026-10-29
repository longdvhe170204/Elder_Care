package com.eldercare.backend.medication.api.dto;

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
