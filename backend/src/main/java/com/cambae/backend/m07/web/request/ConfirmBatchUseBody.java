package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ConfirmBatchUseBody {

    @NotEmpty(message = "Prescription IDs list is required")
    @JsonAlias({"donThuocIds", "prescriptionIds"})
    private List<Long> prescriptionIds;

    @JsonAlias({"thuocId", "medicationId"})
    private Long medicationId;

    @NotNull(message = "Packaging recognizable flag is required")
    @JsonAlias({"baoBiNhanDienDuoc", "packagingRecognizable"})
    private Boolean packagingRecognizable;

    @JsonAlias({"canCu", "basis"})
    private String basis; // TOA_GIA_DINH, DON_CUA_VIEN

    public List<Long> getPrescriptionIds() {
        return prescriptionIds;
    }

    public void setPrescriptionIds(List<Long> prescriptionIds) {
        this.prescriptionIds = prescriptionIds;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public Boolean getPackagingRecognizable() {
        return packagingRecognizable;
    }

    public void setPackagingRecognizable(Boolean packagingRecognizable) {
        this.packagingRecognizable = packagingRecognizable;
    }

    public String getBasis() {
        return basis;
    }

    public void setBasis(String basis) {
        this.basis = basis;
    }
}
