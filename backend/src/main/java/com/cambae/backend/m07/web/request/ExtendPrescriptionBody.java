package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request body for extending a prescription (FR-015, BR-M07-08).
 */
public class ExtendPrescriptionBody {

    @NotBlank
    @Size(max = 2000)
    @JsonAlias({"lyDo", "reason"})
    private String reason;

    @JsonAlias({"ngayKetThucMoi", "newEndedAt"})
    private LocalDate newEndedAt;

    @JsonAlias({"canCuCoSoKe", "externalBasis"})
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

    // Backward compatibility
    public String getLyDo() {
        return reason;
    }

    public void setLyDo(String lyDo) {
        this.reason = lyDo;
    }

    public LocalDate getNgayKetThucMoi() {
        return newEndedAt;
    }

    public void setNgayKetThucMoi(LocalDate ngayKetThucMoi) {
        this.newEndedAt = ngayKetThucMoi;
    }

    public ExternalBasisBody getCanCuCoSoKe() {
        return externalBasis;
    }

    public void setCanCuCoSoKe(ExternalBasisBody canCuCoSoKe) {
        this.externalBasis = canCuCoSoKe;
    }
}
