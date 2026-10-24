package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class HandoverItemBody {

    @NotNull
    @JsonAlias({"donThuocId", "prescriptionId"})
    private Long prescriptionId;

    @NotNull
    @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
    @JsonAlias({"soLuong", "quantity"})
    private BigDecimal quantity;

    @JsonAlias({"loThuocGuiId", "familyMedicationBatchId"})
    private Long familyMedicationBatchId;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getFamilyMedicationBatchId() {
        return familyMedicationBatchId;
    }

    public void setFamilyMedicationBatchId(Long familyMedicationBatchId) {
        this.familyMedicationBatchId = familyMedicationBatchId;
    }
}
