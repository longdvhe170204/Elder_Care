package com.cambae.backend.m07.internal.service;

/**
 * Active allergy item view from health records (001 FR-018, FR-018a, BR-M01-12).
 */
public record AllergyItemView(
        long allergyId, Long allergenId, String allergenName, String severity, boolean fromCatalog) {

    public static AllergyItemView fromCatalog(long allergyId, long allergenId, String allergenName, String severity) {
        return new AllergyItemView(allergyId, allergenId, allergenName, severity, true);
    }

    public static AllergyItemView fromFreeText(long allergyId, String description, String severity) {
        return new AllergyItemView(allergyId, null, description, severity, false);
    }
}
