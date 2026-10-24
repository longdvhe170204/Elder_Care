package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a new prescription (schema DonThuocMoi in api-006.yaml).
 * Standardized English field names with backward-compatible Vietnamese aliases.
 */
public class CreatePrescriptionBody {

    @NotNull
    @JsonAlias({"thuocId"})
    private Long medicationId;

    @NotNull
    @DecimalMin("0.01")
    @JsonAlias({"lieu"})
    private BigDecimal dosage;

    @JsonAlias({"donVi", "unit"})
    private String dosageUnit;

    @NotEmpty
    @JsonAlias({"duongDung"})
    private String route;

    @NotEmpty
    @JsonAlias({"loaiDon"})
    private String prescriptionType; // PERIODIC | PRN | DINH_KY

    @Valid
    @JsonAlias({"tanSuat"})
    private FrequencyBody frequency;

    @JsonAlias({"khoangCachToiThieuPhut"})
    private Integer minIntervalMinutes;

    @JsonAlias({"soLanToiDa24h"})
    private Integer maxDosesPer24h;

    @NotNull
    @JsonAlias({"batDauLuc"})
    private Instant startedAt;

    @JsonAlias({"ngayKetThuc", "endDate"})
    private LocalDate endedAt;

    @NotEmpty
    @JsonAlias({"nguonThuoc", "source"})
    private String medicationSource; // FACILITY | FAMILY_SENT | VIEN | GIA_DINH_GUI

    @NotNull
    @Valid
    @JsonAlias({"coSoKe"})
    private PrescriberBody prescriber;

    @JsonAlias({"ghiChuHuongDan", "dosageInstruction"})
    private String instructionNote;

    @JsonAlias({"dauChoTiepNhan"})
    private boolean pendingAdmission;

    @Valid
    @JsonAlias({"xacNhanVanDe"})
    private List<ConfirmedIssueBody> confirmedIssues;

    // --- English Getters & Setters ---

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public void setDosage(BigDecimal dosage) {
        this.dosage = dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getPrescriptionType() {
        return prescriptionType;
    }

    public void setPrescriptionType(String prescriptionType) {
        this.prescriptionType = prescriptionType;
    }

    public FrequencyBody getFrequency() {
        return frequency;
    }

    public void setFrequency(FrequencyBody frequency) {
        this.frequency = frequency;
    }

    public Integer getMinIntervalMinutes() {
        return minIntervalMinutes;
    }

    public void setMinIntervalMinutes(Integer minIntervalMinutes) {
        this.minIntervalMinutes = minIntervalMinutes;
    }

    public Integer getMaxDosesPer24h() {
        return maxDosesPer24h;
    }

    public void setMaxDosesPer24h(Integer maxDosesPer24h) {
        this.maxDosesPer24h = maxDosesPer24h;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDate getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDate endedAt) {
        this.endedAt = endedAt;
    }

    public String getMedicationSource() {
        return medicationSource;
    }

    public void setMedicationSource(String medicationSource) {
        this.medicationSource = medicationSource;
    }

    public PrescriberBody getPrescriber() {
        return prescriber;
    }

    public void setPrescriber(PrescriberBody prescriber) {
        this.prescriber = prescriber;
    }

    public String getInstructionNote() {
        return instructionNote;
    }

    public void setInstructionNote(String instructionNote) {
        this.instructionNote = instructionNote;
    }

    public boolean isPendingAdmission() {
        return pendingAdmission;
    }

    public void setPendingAdmission(boolean pendingAdmission) {
        this.pendingAdmission = pendingAdmission;
    }

    public List<ConfirmedIssueBody> getConfirmedIssues() {
        return confirmedIssues;
    }

    public void setConfirmedIssues(List<ConfirmedIssueBody> confirmedIssues) {
        this.confirmedIssues = confirmedIssues;
    }

    // --- Backward-Compatible Vietnamese Getters & Setters ---

    public Long getThuocId() {
        return medicationId;
    }

    public void setThuocId(Long thuocId) {
        this.medicationId = thuocId;
    }

    public BigDecimal getLieu() {
        return dosage;
    }

    public void setLieu(BigDecimal lieu) {
        this.dosage = lieu;
    }

    public String getDonVi() {
        return dosageUnit;
    }

    public void setDonVi(String donVi) {
        this.dosageUnit = donVi;
    }

    public String getDuongDung() {
        return route;
    }

    public void setDuongDung(String duongDung) {
        this.route = duongDung;
    }

    public String getLoaiDon() {
        return prescriptionType;
    }

    public void setLoaiDon(String loaiDon) {
        this.prescriptionType = loaiDon;
    }

    public FrequencyBody getTanSuat() {
        return frequency;
    }

    public void setTanSuat(FrequencyBody tanSuat) {
        this.frequency = tanSuat;
    }

    public Integer getKhoangCachToiThieuPhut() {
        return minIntervalMinutes;
    }

    public void setKhoangCachToiThieuPhut(Integer khoangCachToiThieuPhut) {
        this.minIntervalMinutes = khoangCachToiThieuPhut;
    }

    public Integer getSoLanToiDa24h() {
        return maxDosesPer24h;
    }

    public void setSoLanToiDa24h(Integer soLanToiDa24h) {
        this.maxDosesPer24h = soLanToiDa24h;
    }

    public Instant getBatDauLuc() {
        return startedAt;
    }

    public void setBatDauLuc(Instant batDauLuc) {
        this.startedAt = batDauLuc;
    }

    public LocalDate getNgayKetThuc() {
        return endedAt;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.endedAt = ngayKetThuc;
    }

    public String getNguonThuoc() {
        return medicationSource;
    }

    public void setNguonThuoc(String nguonThuoc) {
        this.medicationSource = nguonThuoc;
    }

    public PrescriberBody getCoSoKe() {
        return prescriber;
    }

    public void setCoSoKe(PrescriberBody coSoKe) {
        this.prescriber = coSoKe;
    }

    public String getGhiChuHuongDan() {
        return instructionNote;
    }

    public void setGhiChuHuongDan(String ghiChuHuongDan) {
        this.instructionNote = ghiChuHuongDan;
    }

    public boolean isDauChoTiepNhan() {
        return pendingAdmission;
    }

    public void setDauChoTiepNhan(boolean dauChoTiepNhan) {
        this.pendingAdmission = dauChoTiepNhan;
    }

    public List<ConfirmedIssueBody> getXacNhanVanDe() {
        return confirmedIssues;
    }

    public void setXacNhanVanDe(List<ConfirmedIssueBody> xacNhanVanDe) {
        this.confirmedIssues = xacNhanVanDe;
    }
}
