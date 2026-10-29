package com.eldercare.backend.medication.api;

import com.eldercare.backend.medication.api.dto.FamilyMedicationView;
import java.util.List;

/**
 * API for querying family-sent medications.
 * Used by Module M02 (004 - Billing & Discharge), M10 (012), M14 (016).
 */
public interface FamilySentMedicationApi {
    /** Check whether the facility is retaining family-sent medication for the resident (004 FR-063c). */
    boolean hasRetainedMedication(long residentId);

    /** List of family-sent medications of the resident (name, status, remaining quantity, expiry date). */
    List<FamilyMedicationView> byResident(long residentId);
}
