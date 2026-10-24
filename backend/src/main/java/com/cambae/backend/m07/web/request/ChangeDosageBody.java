package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"donThayThe", "newPrescription"})
    private CreatePrescriptionBody newPrescription;

    @NotNull
    @JsonAlias({"hieuLucTu", "effectiveFrom"})
    private Instant effectiveFrom;

    @NotBlank
    @Size(max = 2000)
    @JsonAlias({"lyDo", "reason"})
    private String reason;

    @JsonAlias({"canCuCoSoKe", "externalBasis"})
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

    // Backward compatibility
    public CreatePrescriptionBody getDonThayThe() {
        return newPrescription;
    }

    public void setDonThayThe(CreatePrescriptionBody donThayThe) {
        this.newPrescription = donThayThe;
    }

    public Instant getHieuLucTu() {
        return effectiveFrom;
    }

    public void setHieuLucTu(Instant hieuLucTu) {
        this.effectiveFrom = hieuLucTu;
    }

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
