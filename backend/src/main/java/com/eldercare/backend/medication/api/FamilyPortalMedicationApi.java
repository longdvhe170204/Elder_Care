package com.eldercare.backend.medication.api;

import com.eldercare.backend.medication.api.dto.ConfirmedReconciliationView;
import com.eldercare.backend.medication.api.dto.FamilyMedicationView;
import com.eldercare.backend.medication.api.dto.FamilyPrescriptionView;
import java.util.List;
import java.util.Optional;

/**
 * API dedicated to the Family Portal (FR-049, Q-59).
 * Used by Module M10 (012 - Family Portal).
 */
public interface FamilyPortalMedicationApi {
    /** List of medications sent to the facility by family members (for family member with Active relationship). */
    List<FamilyMedicationView> sentMedications(long residentId, long familyMemberId);

    /** Prescriptions of the resident (only returned when consent covers the family member). */
    Optional<List<FamilyPrescriptionView>> prescriptions(long residentId, long familyMemberId);

    /** Confirmed medication reconciliations (only returned when consent covers the family member). */
    Optional<List<ConfirmedReconciliationView>> confirmedReconciliations(long residentId, long familyMemberId);
}
