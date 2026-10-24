package com.cambae.backend.m07.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrnAdministrationResponse {

    private Long id;

    @JsonProperty("donThuocId")
    private Long prescriptionId;

    @JsonProperty("nctId")
    private Long residentId;

    @JsonProperty("thoiDiemDung")
    private Instant administeredAt;

    @JsonProperty("lieuDung")
    private BigDecimal dosage;

    @JsonProperty("donVi")
    private String dosageUnit;

    @JsonProperty("lyDo")
    private String reason;

    @JsonProperty("nguoiThucHienId")
    private Long administeredById;

    @JsonProperty("nguoiBao")
    private String reportedBy;

    @JsonProperty("vuotGioiHan")
    private boolean exceededLimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public Instant getAdministeredAt() {
        return administeredAt;
    }

    public void setAdministeredAt(Instant administeredAt) {
        this.administeredAt = administeredAt;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getAdministeredById() {
        return administeredById;
    }

    public void setAdministeredById(Long administeredById) {
        this.administeredById = administeredById;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public boolean isExceededLimit() {
        return exceededLimit;
    }

    public void setExceededLimit(boolean exceededLimit) {
        this.exceededLimit = exceededLimit;
    }
}
