package com.eldercare.backend.medication.internal.service.request;

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
 */
public class CreatePrescriptionBody {

    @NotNull
    private Long medicationId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal dosage;

    private String dosageUnit;

    @NotEmpty
    private String route;

    @NotEmpty
    private String prescriptionType; // PERIODIC | PRN

    @Valid
    private FrequencyBody frequency;

    private Integer minIntervalMinutes;

    private Integer maxDosesPer24h;

    @NotNull
    private Instant startedAt;

    private LocalDate endedAt;

    @NotEmpty
    private String medicationSource; // FACILITY | FAMILY_SENT

    @NotNull
    @Valid
    private PrescriberBody prescriber;

    private String instructionNote;

    private boolean pendingAdmission;

    @Valid
    private List<ConfirmedIssueBody> confirmedIssues;

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

    public String getUnit() {
        return dosageUnit;
    }

    public void setUnit(String unit) {
        this.dosageUnit = unit;
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
}
