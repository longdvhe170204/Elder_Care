package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.Instant;
import java.time.LocalDate;

/**
 * External prescriber basis for nursing orders on outside prescriptions (FR-013, BR-M07-16).
 */
public class ExternalBasisBody {

    @JsonAlias({"tenCoSo", "facilityName"})
    private String facilityName;

    @JsonAlias({"nguoiChiDinh", "prescriberName"})
    private String prescriberName;

    @JsonAlias({"ngay", "prescribedDate"})
    private LocalDate prescribedDate;

    @JsonAlias({"hinhThuc", "basisType"})
    private String basisType;

    @JsonAlias({"nguoiGoi", "callerName"})
    private String callerName;

    @JsonAlias({"nguoiTraLoi", "receiverName"})
    private String receiverName;

    @JsonAlias({"thoiDiemGoi", "calledAt"})
    private Instant calledAt;

    @JsonAlias({"tepId", "documentFileId"})
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
