package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Dose Result Entity (Group 3 - Recording append-only; DBR-13, BR-M07-12; data-model section 5).
 * Table {@code dose_result}. Each closed dose has exactly one original record; no updates, no deletes.
 */
@Entity
@Table(name = "dose_result")
@DataGroup(DataGroup.Group.RECORD)
public class DoseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dose_id", nullable = false, unique = true)
    private Long doseId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "floor_area_id")
    private Long floorAreaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 30)
    private DoseResultType resultType;

    @Column(name = "administered_at")
    private Instant administeredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "device_recorded_at")
    private Instant deviceRecordedAt;

    @Column(name = "recorder_id")
    private Long recorderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "authority_basis", nullable = false, length = 50)
    private DoseAuthorityBasis authorityBasis;

    @Column(name = "reported_by_type", length = 30)
    private String reportedByType;

    @Column(name = "reported_by_id")
    private Long reportedById;

    @Column(name = "reported_by_name")
    private String reportedByName;

    @Column(name = "relationship", length = 100)
    private String relationship;

    @Column(name = "adverse_reaction", length = 2000)
    private String adverseReaction;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "late_flag", nullable = false)
    private boolean lateFlag = false;

    @Column(name = "offline_flag", nullable = false)
    private boolean offlineFlag = false;

    @Column(name = "synced_after_permission_loss", nullable = false)
    private boolean syncedAfterPermissionLoss = false;

    @Column(name = "family_medication_lot_id")
    private Long familyMedicationLotId;

    @Column(name = "leave_handover_id")
    private Long leaveHandoverId;

    protected DoseResult() {}

    public DoseResult(
            Long doseId,
            Long residentId,
            Long floorAreaId,
            DoseResultType resultType,
            Instant administeredAt,
            Instant deviceRecordedAt,
            Long recorderId,
            DoseAuthorityBasis authorityBasis,
            String reportedByType,
            Long reportedById,
            String reportedByName,
            String relationship,
            String adverseReaction,
            String reason,
            String note,
            boolean lateFlag,
            boolean offlineFlag,
            boolean syncedAfterPermissionLoss,
            Long familyMedicationLotId,
            Long leaveHandoverId,
            Instant now) {
        this.doseId = doseId;
        this.residentId = residentId;
        this.floorAreaId = floorAreaId;
        this.resultType = resultType;
        this.administeredAt = administeredAt;
        this.recordedAt = now;
        this.deviceRecordedAt = deviceRecordedAt;
        this.recorderId = recorderId;
        this.authorityBasis = authorityBasis;
        this.reportedByType = reportedByType;
        this.reportedById = reportedById;
        this.reportedByName = reportedByName;
        this.relationship = relationship;
        this.adverseReaction = adverseReaction;
        this.reason = reason;
        this.note = note;
        this.lateFlag = lateFlag;
        this.offlineFlag = offlineFlag;
        this.syncedAfterPermissionLoss = syncedAfterPermissionLoss;
        this.familyMedicationLotId = familyMedicationLotId;
        this.leaveHandoverId = leaveHandoverId;
    }

    public Long getId() {
        return id;
    }

    public Long getDoseId() {
        return doseId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getFloorAreaId() {
        return floorAreaId;
    }

    public DoseResultType getResultType() {
        return resultType;
    }

    public Instant getAdministeredAt() {
        return administeredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public Instant getDeviceRecordedAt() {
        return deviceRecordedAt;
    }

    public Long getRecorderId() {
        return recorderId;
    }

    public DoseAuthorityBasis getAuthorityBasis() {
        return authorityBasis;
    }

    public String getReportedByType() {
        return reportedByType;
    }

    public Long getReportedById() {
        return reportedById;
    }

    public String getReportedByName() {
        return reportedByName;
    }

    public String getRelationship() {
        return relationship;
    }

    public String getAdverseReaction() {
        return adverseReaction;
    }

    public String getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public boolean isLateFlag() {
        return lateFlag;
    }

    public boolean isOfflineFlag() {
        return offlineFlag;
    }

    public boolean isSyncedAfterPermissionLoss() {
        return syncedAfterPermissionLoss;
    }

    public Long getFamilyMedicationLotId() {
        return familyMedicationLotId;
    }

    public Long getLeaveHandoverId() {
        return leaveHandoverId;
    }
}
