package com.cambae.backend.m07.api.dto;

/**
 * Medication summary view.
 * Belongs to the HEALTH information category; may be masked depending on user view permissions.
 */
public record MedicationSummary(
        long medicationId, String name, String activeIngredient, String strength, String dosageForm) {}
