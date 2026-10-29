package com.eldercare.backend.medication.internal.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for suspending or resuming a prescription order (FR-011, FR-013).
 */
public class OrderPrescriptionBody {

    @NotBlank
    @Size(max = 2000)
    private String reason;

    private ExternalBasisBody externalBasis;

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
