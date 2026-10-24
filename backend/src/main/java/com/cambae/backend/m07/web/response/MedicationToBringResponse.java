package com.cambae.backend.m07.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicationToBringResponse {

    @JsonProperty("donThuocId")
    private Long prescriptionId;

    @JsonProperty("tenThuoc")
    private String medicationName;

    @JsonProperty("lieuDung")
    private BigDecimal dosage;

    @JsonProperty("donVi")
    private String dosageUnit;

    @JsonProperty("loaiDon")
    private String prescriptionType; // PERIODIC | PRN

    @JsonProperty("soLuongCanMang")
    private BigDecimal totalQuantityNeeded;

    @JsonProperty("thoiDiemDuKien")
    private List<Instant> scheduledTimes = new ArrayList<>();

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
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

    public String getPrescriptionType() {
        return prescriptionType;
    }

    public void setPrescriptionType(String prescriptionType) {
        this.prescriptionType = prescriptionType;
    }

    public BigDecimal getTotalQuantityNeeded() {
        return totalQuantityNeeded;
    }

    public void setTotalQuantityNeeded(BigDecimal totalQuantityNeeded) {
        this.totalQuantityNeeded = totalQuantityNeeded;
    }

    public List<Instant> getScheduledTimes() {
        return scheduledTimes;
    }

    public void setScheduledTimes(List<Instant> scheduledTimes) {
        this.scheduledTimes = scheduledTimes != null ? scheduledTimes : new ArrayList<>();
    }
}
