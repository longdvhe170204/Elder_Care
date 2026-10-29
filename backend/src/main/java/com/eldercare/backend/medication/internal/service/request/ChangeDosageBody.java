package com.eldercare.backend.medication.internal.service.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request body for changing dosage or medication (FR-016, BR-M07-07, DBR-14).
 */
public class ChangeDosageBody {

    @NotNull
    @Valid
    private CreatePrescriptionBody newPrescription;

    @NotNull
    private Instant effectiveFrom;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    private ExternalBasisBody externalBasis;

    public CreatePrescriptionBody getNewPrescription() {
        return newPrescription;
    }

    public void setNewPrescription(CreatePrescriptionBody newPrescription) {
        this.newPrescription = newPrescription;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ExternalBasisBody getExternalBasis() {
        return externalBasis;
    }

    public void setExternalBasis(ExternalBasisBody externalBasis) {
        this.externalBasis = externalBasis;
    }
}
