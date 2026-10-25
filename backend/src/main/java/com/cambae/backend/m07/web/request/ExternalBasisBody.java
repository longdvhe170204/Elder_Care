package com.cambae.backend.m07.web.request;

import java.time.Instant;
import java.time.LocalDate;

/**
 * External prescriber basis for nursing orders on outside prescriptions (FR-013, BR-M07-16).
 */
public class ExternalBasisBody {

    private String facilityName;

    private String prescriberName;

    private LocalDate prescribedDate;

    private String basisType;

    private String callerName;

    private String receiverName;

    private Instant calledAt;

    private Long documentFileId;

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

    public LocalDate getPrescribedDate() {
        return prescribedDate;
    }

    public void setPrescribedDate(LocalDate prescribedDate) {
        this.prescribedDate = prescribedDate;
    }

    public String getBasisType() {
        return basisType;
    }

    public void setBasisType(String basisType) {
        this.basisType = basisType;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Instant calledAt) {
        this.calledAt = calledAt;
    }

    public Long getDocumentFileId() {
        return documentFileId;
    }

    public void setDocumentFileId(Long documentFileId) {
        this.documentFileId = documentFileId;
    }
}
