package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request body for discontinuing a prescription (immediately or from a future time) (FR-011, FR-015, FR-016).
 */
public class DiscontinuePrescriptionBody {

    @NotBlank
    @Size(max = 2000)
    private String reason;

    private Instant discontinueAt;

    private ExternalBasisBody externalBasis;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getDiscontinueAt() {
        return discontinueAt;
    }

    public void setDiscontinueAt(Instant discontinueAt) {
        this.discontinueAt = discontinueAt;
    }

    public ExternalBasisBody getExternalBasis() {
        return externalBasis;
    }

    public void setExternalBasis(ExternalBasisBody externalBasis) {
        this.externalBasis = externalBasis;
    }
}
