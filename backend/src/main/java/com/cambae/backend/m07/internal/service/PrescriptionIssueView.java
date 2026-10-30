package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.internal.domain.PrescriptionIssueType;

/**
 * View of an issue returned from prescription verification (FR-007, FR-008; components/schemas/VanDe in api-006.yaml).
 */
public record PrescriptionIssueView(
        String issueCode,
        PrescriptionIssueType issueType,
        String activeIngredient,
        Long duplicatePrescriptionId,
        Long allergyItemId,
        String severity,
        boolean manualReviewRequired,
        Integer lineNumber) {

    public static PrescriptionIssueView duplicateIngredient(
            long duplicatePrescriptionId, long allergenId, String ingredientName) {
        return new PrescriptionIssueView(
                "DUPLICATE_INGREDIENT_" + duplicatePrescriptionId + "_" + allergenId,
                PrescriptionIssueType.DUPLICATE_ACTIVE_INGREDIENT,
                ingredientName != null ? ingredientName : String.valueOf(allergenId),
                duplicatePrescriptionId,
                null,
                null,
                false,
                null);
    }

    public static PrescriptionIssueView allergyConflict(long allergyItemId, String allergenName, String severity) {
        return new PrescriptionIssueView(
                "ALLERGY_" + allergyItemId,
                PrescriptionIssueType.ALLERGY_CONFLICT,
                allergenName,
                null,
                allergyItemId,
                severity,
                false,
                null);
    }

    public static PrescriptionIssueView otherAllergyReview(long allergyItemId, String description, String severity) {
        return new PrescriptionIssueView(
                "OTHER_ALLERGY_" + allergyItemId,
                PrescriptionIssueType.OTHER_ALLERGY_REVIEW,
                description,
                null,
                allergyItemId,
                severity,
                true,
                null);
    }
}
