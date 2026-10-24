package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for suspending or resuming a prescription order (FR-011, FR-013).
 */
public class OrderPrescriptionBody {

    @NotBlank
    @Size(max = 2000)
    @JsonAlias({"lyDo", "reason"})
    private String reason;

    @JsonAlias({"canCuCoSoKe", "externalBasis"})
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

    // Backward compatibility
    public String getLyDo() {
        return reason;
    }

    public void setLyDo(String lyDo) {
        this.reason = lyDo;
    }

    public ExternalBasisBody getCanCuCoSoKe() {
        return externalBasis;
    }

    public void setCanCuCoSoKe(ExternalBasisBody canCuCoSoKe) {
        this.externalBasis = canCuCoSoKe;
    }
}
