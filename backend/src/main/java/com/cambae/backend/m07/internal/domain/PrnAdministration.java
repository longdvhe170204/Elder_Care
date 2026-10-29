package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * PRN medication administration entity (FR-024, FR-025, BR-M07-04).
 */
@Entity
@DataGroup(DataGroup.Group.RECORD)
@Table(name = "prn_administration")
public class PrnAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "administered_at", nullable = false)
    private Instant administeredAt;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal dosage;

    @Column(name = "dosage_unit", nullable = false, length = 30)
    private String dosageUnit;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(name = "adverse_reaction", length = 2000)
    private String adverseReaction;

    @Column(length = 2000)
    private String note;

    @Column(name = "recorded_after_disconnect", nullable = false)
    private boolean recordedAfterDisconnect;

    @Column(name = "administered_by_id", nullable = false)
    private Long administeredById;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "exceeded_limit", nullable = false)
    private boolean exceededLimit = false;

    @Column(name = "offline_device_time")
    private Instant offlineDeviceTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PrnAdministration() {}

    public PrnAdministration(
            Long prescriptionId,
            Long residentId,
            Instant administeredAt,
            BigDecimal dosage,
            String dosageUnit,
            String reason,
            Long administeredById,
            String reportedBy,
            boolean exceededLimit,
            Instant offlineDeviceTime,
            String adverseReaction,
            String note,
            boolean recordedAfterDisconnect,
            Instant now) {
        this.prescriptionId = prescriptionId;
        this.adverseReaction = adverseReaction;
        this.note = note;
        this.recordedAfterDisconnect = recordedAfterDisconnect;
        this.residentId = residentId;
        this.administeredAt = administeredAt != null ? administeredAt : now;
        this.dosage = dosage;
        this.dosageUnit = dosageUnit;
        this.reason = reason;
        this.administeredById = administeredById;
        this.reportedBy = reportedBy;
        this.exceededLimit = exceededLimit;
        this.offlineDeviceTime = offlineDeviceTime;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Instant getAdministeredAt() {
        return administeredAt;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public String getReason() {
        return reason;
    }

    public Long getAdministeredById() {
        return administeredById;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public boolean isExceededLimit() {
        return exceededLimit;
    }

    public Instant getOfflineDeviceTime() {
        return offlineDeviceTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAdverseReaction() {
        return adverseReaction;
    }

    public String getNote() {
        return note;
    }

    public boolean isRecordedAfterDisconnect() {
        return recordedAfterDisconnect;
    }
}
