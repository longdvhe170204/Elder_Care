package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.ActiveMedicationView;
import com.cambae.backend.m07.api.dto.PrescriptionView;
import java.util.List;

/**
 * API for reading prescription details.
 * Used by Module M05/M06 (007 - Emergency Card, Transfer) and M14 (016 - Reports).
 */
public interface PrescriptionApi {
    /** List of unended facility-source prescriptions (Active, Suspended, Pending Active). */
    List<PrescriptionView> unendedFacilityPrescriptions(long residentId);

    /** Currently active medications for emergency cards and transfer summaries (FR-050). */
    List<ActiveMedicationView> activeMedications(long residentId);

    /** Prescription replacement history chain over time (FR-016a). */
    List<PrescriptionView> history(long residentId);
}
