package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;

/**
 * Prescriber facility and professional info (schema CoSoKe in api-006.yaml).
 * Standardized English field names with backward-compatible Vietnamese aliases.
 */
public class PrescriberBody {

    @NotEmpty
    @JsonAlias({"loai"})
    private String type; // INTERNAL | EXTERNAL | NOI_BO | BEN_NGOAI

    @JsonAlias({"tenCoSo"})
    private String facilityName;

    @JsonAlias({"nguoiKe"})
    private String prescriberName;

    @JsonAlias({"ngayKe", "prescribedDate"})
    private LocalDate prescribedAt;

    @JsonAlias({"nguoiKeId", "doctorStaffId"})
    private Long prescriberId;

    @JsonAlias({"tepDonId"})
    private Long prescriptionFileId;

    // --- English Getters & Setters ---

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

    // --- Backward-Compatible Vietnamese Getters & Setters ---

    public String getLoai() {
        return type;
    }

    public void setLoai(String loai) {
        this.type = loai;
    }

    public String getTenCoSo() {
        return facilityName;
    }

    public void setTenCoSo(String tenCoSo) {
        this.facilityName = tenCoSo;
    }

    public String getNguoiKe() {
        return prescriberName;
    }

    public void setNguoiKe(String nguoiKe) {
        this.prescriberName = nguoiKe;
    }

    public LocalDate getNgayKe() {
        return prescribedAt;
    }

    public void setNgayKe(LocalDate ngayKe) {
        this.prescribedAt = ngayKe;
    }

    public Long getNguoiKeId() {
        return prescriberId;
    }

    public void setNguoiKeId(Long nguoiKeId) {
        this.prescriberId = nguoiKeId;
    }

    public Long getTepDonId() {
        return prescriptionFileId;
    }

    public void setTepDonId(Long tepDonId) {
        this.prescriptionFileId = tepDonId;
    }
}
