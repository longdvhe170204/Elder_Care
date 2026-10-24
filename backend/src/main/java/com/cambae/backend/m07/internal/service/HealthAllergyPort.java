package com.cambae.backend.m07.internal.service;

import java.util.List;

/**
 * Health / allergy lookup port from Module M01 (001) per FR-007, FR-008, FR-012.
 */
public interface HealthAllergyPort {

    /**
     * Gets the list of active allergies for a resident.
     */
    List<AllergyItemView> getActiveAllergies(long residentId);

    /**
     * Notifies M01 that the allergy item has been checked against active prescriptions (001 BR-M01-12).
     */
    void markAllergyChecked(long allergyId, String module);
}
