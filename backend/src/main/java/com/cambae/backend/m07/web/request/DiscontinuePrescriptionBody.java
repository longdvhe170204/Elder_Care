package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request body for discontinuing a prescription (immediately or from a future time) (FR-011, FR-015, FR-016).
 */
public class DiscontinuePrescriptionBody {

    @NotBlank
    @Size(max = 2000)
    @JsonAlias({"lyDo", "reason"})
    private String reason;

    @JsonAlias({"ngungTu", "discontinueAt"})
    private Instant discontinueAt;

    @JsonAlias({"canCuCoSoKe", "externalBasis"})
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

    // Backward compatibility
    public String getLyDo() {
        return reason;
    }

    public void setLyDo(String lyDo) {
        this.reason = lyDo;
    }

    public Instant getNgungTu() {
        return discontinueAt;
    }

    public void setNgungTu(Instant ngungTu) {
        this.discontinueAt = ngungTu;
    }

    public ExternalBasisBody getCanCuCoSoKe() {
        return externalBasis;
    }

    public void setCanCuCoSoKe(ExternalBasisBody canCuCoSoKe) {
        this.externalBasis = canCuCoSoKe;
    }
}
