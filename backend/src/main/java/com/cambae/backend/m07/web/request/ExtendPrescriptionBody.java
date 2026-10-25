package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request body for extending a prescription (FR-015, BR-M07-08).
 */
public class ExtendPrescriptionBody {

    @NotBlank
    @Size(max = 2000)
    private String reason;

    private LocalDate newEndedAt;

    private ExternalBasisBody externalBasis;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getNewEndedAt() {
        return newEndedAt;
    }

    public void setNewEndedAt(LocalDate newEndedAt) {
        this.newEndedAt = newEndedAt;
    }

    public ExternalBasisBody getExternalBasis() {
        return externalBasis;
    }

    public void setExternalBasis(ExternalBasisBody externalBasis) {
        this.externalBasis = externalBasis;
    }
}
