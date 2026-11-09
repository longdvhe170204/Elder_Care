package com.eldercare.backend.medication.internal.reconciliation.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/** Trạng thái kỹ thuật theo người cao tuổi của M07 trong {@code resident_module_state}. */
public record MedicationResidentState(
        Instant reconciliationDueAt, String reconciliationReason, Instant reconciliationWarnedAt) {

    public static final SharedKind<MedicationResidentState> KIND =
            SharedKind.of("M07.MEDICATION_STATE", MedicationResidentState.class).notDeletable();

    public static MedicationResidentState dueAt(Instant dueAt, String reason) {
        return new MedicationResidentState(dueAt, reason, null);
    }

    public MedicationResidentState warnedAt(Instant warnedAt) {
        return new MedicationResidentState(reconciliationDueAt, reconciliationReason, warnedAt);
    }

    public static MedicationResidentState empty() {
        return new MedicationResidentState(null, null, null);
    }
}
