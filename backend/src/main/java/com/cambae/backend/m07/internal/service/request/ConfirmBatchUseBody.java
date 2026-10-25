package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ConfirmBatchUseBody {

    @NotEmpty(message = "Prescription IDs list is required")
    private List<Long> prescriptionIds;

    private Long medicationId;

    @NotNull(message = "Packaging recognizable flag is required")
    private Boolean packagingRecognizable;

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
