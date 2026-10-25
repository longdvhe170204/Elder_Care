package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;

/**
 * Prescriber facility and professional info (schema CoSoKe in api-006.yaml).
 */
public class PrescriberBody {

    @NotEmpty
    private String type; // INTERNAL | EXTERNAL

    private String facilityName;

    private String prescriberName;

    private LocalDate prescribedAt;

    private Long prescriberId;

    private Long prescriptionFileId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public void setPrescriberName(String prescriberName) {
        this.prescriberName = prescriberName;
    }

    public LocalDate getPrescribedAt() {
        return prescribedAt;
    }

    public void setPrescribedAt(LocalDate prescribedAt) {
        this.prescribedAt = prescribedAt;
    }

    public Long getPrescriberId() {
        return prescriberId;
    }

    public void setPrescriberId(Long prescriberId) {
        this.prescriberId = prescriberId;
    }

    public Long getPrescriptionFileId() {
        return prescriptionFileId;
    }

    public void setPrescriptionFileId(Long prescriptionFileId) {
        this.prescriptionFileId = prescriptionFileId;
    }
}
