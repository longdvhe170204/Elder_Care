package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Pure function to check active ingredient duplication and allergy conflicts for new prescriptions (FR-007, FR-008, BR-M07-06; research D-05).
 * Shared across: new prescription creation, dosage changes (skipping replaced prescription), and reconciliation line checks.
 */
@Component
public class PrescriptionValidator {

    /**
     * Checks active ingredient duplication and allergy conflicts.
     *
     * @param newAllergenIds         Active ingredient allergen IDs of the new medication
     * @param existingPrescriptions  Existing prescriptions of the resident in ACTIVE, SUSPENDED, or PENDING_EFFECTIVE status
     * @param activeAllergies        Active allergy items of the resident
     * @param replacedPrescriptionId Prescription ID being replaced (if any, skipped during dosage changes)
     * @return Result containing blocking issues and review items
     */
    public PrescriptionCheckResult validate(
            Set<Long> newAllergenIds,
            Collection<Prescription> existingPrescriptions,
            Collection<AllergyItemView> activeAllergies,
            Long replacedPrescriptionId) {

        List<PrescriptionIssueView> issues = new ArrayList<>();
        List<PrescriptionIssueView> reviewItems = new ArrayList<>();

        if (newAllergenIds == null) {
            newAllergenIds = Set.of();
        }

        // 1. Check duplicate active ingredient against unended prescriptions (excluding replaced prescription)
        if (existingPrescriptions != null) {
            for (Prescription prescription : existingPrescriptions) {
                if (replacedPrescriptionId != null && Objects.equals(prescription.getId(), replacedPrescriptionId)) {
                    continue; // Skip replaced prescription itself (BR-M07-06 scenario 4)
                }

                // Only consider prescriptions in ACTIVE, SUSPENDED, PENDING_EFFECTIVE
                if (prescription.getStatus() != PrescriptionStatus.ACTIVE
                        && prescription.getStatus() != PrescriptionStatus.SUSPENDED
                        && prescription.getStatus() != PrescriptionStatus.PENDING_EFFECTIVE) {
                    continue;
                }

                Set<Long> intersection = new HashSet<>(prescription.getAllergenIds());
                intersection.retainAll(newAllergenIds);

                for (Long allergenId : intersection) {
                    issues.add(PrescriptionIssueView.duplicateIngredient(prescription.getId(), allergenId, null));
                }
            }
        }

        // 2. Check allergy conflicts
        if (activeAllergies != null) {
            for (AllergyItemView allergy : activeAllergies) {
                if (allergy.fromCatalog() && allergy.allergenId() != null) {
                    if (newAllergenIds.contains(allergy.allergenId())) {
                        issues.add(PrescriptionIssueView.allergyConflict(
                                allergy.allergyId(), allergy.allergenName(), allergy.severity()));
                    }
                } else {
                    // Free-text other allergy -> non-blocking review item (FR-008)
                    reviewItems.add(PrescriptionIssueView.otherAllergyReview(
                            allergy.allergyId(), allergy.allergenName(), allergy.severity()));
                }
            }
        }

        return new PrescriptionCheckResult(issues, reviewItems);
    }
}
