package com.eldercare.backend.medication.internal.reconciliation.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationResidentState;
import org.springframework.stereotype.Component;

/**
 * Khóa bi quan theo người cao tuổi trên {@code resident_module_state} của M07 (research D-03, R-15).
 */
@Component
public class ResidentMedicationLock {

    private final ResidentModuleStateApi states;

    public ResidentMedicationLock(ResidentModuleStateApi states) {
        this.states = states;
    }

    /**
     * Ensure record exists and lock row until the end of the current transaction.
     *
     * @param residentId resident logical ID
     */
    public void lock(long residentId) {
        if (states.lockForUpdate(MedicationResidentState.KIND, residentId).isEmpty()) {
            states.save(MedicationResidentState.KIND, residentId, MedicationResidentState.empty());
            states.lockForUpdate(MedicationResidentState.KIND, residentId);
        }
    }
}
