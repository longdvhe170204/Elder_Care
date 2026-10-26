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
import java.time.LocalDate;

/**
 * External prescriber basis for nurse actions on external prescriptions (FR-013, BR-M07-16; data-model section 4).
 * Group 3 - Append-only. Table: prescription_basis.
 */
@Entity
@Table(name = "prescription_basis")
@DataGroup(DataGroup.Group.RECORD)
public class PrescriptionBasis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command", nullable = false)
    private PrescriptionBasisCommand command;

    @Column(name = "facility_name", nullable = false)
    private String facilityName;

    @Column(name = "prescriber_name", nullable = false)
    private String prescriberName;

    @Column(name = "prescribed_date", nullable = false)
    private LocalDate prescribedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "basis_type", nullable = false)
    private PrescriptionBasisType basisType;

    @Column(name = "caller_name")
    private String callerName;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "document_file_id")
    private Long documentFileId;

    @Column(name = "recorder_id", nullable = false)
    private Long recorderId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected PrescriptionBasis() {}

    public PrescriptionBasis(
            Long prescriptionId,
            PrescriptionBasisCommand command,
            String facilityName,
            String prescriberName,
            LocalDate prescribedDate,
            PrescriptionBasisType basisType,
            String callerName,
            String receiverName,
            Instant calledAt,
            Long documentFileId,
            Long recorderId,
            Instant recordedAt) {
        this.prescriptionId = prescriptionId;
        this.command = command;
        this.facilityName = facilityName;
        this.prescriberName = prescriberName;
        this.prescribedDate = prescribedDate;
        this.basisType = basisType;
        this.callerName = callerName;
        this.receiverName = receiverName;
        this.calledAt = calledAt;
        this.documentFileId = documentFileId;
        this.recorderId = recorderId;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public PrescriptionBasisCommand getCommand() {
        return command;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public LocalDate getPrescribedDate() {
        return prescribedDate;
    }

    public PrescriptionBasisType getBasisType() {
        return basisType;
    }

    public String getCallerName() {
        return callerName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public Long getDocumentFileId() {
        return documentFileId;
    }

    public Long getRecorderId() {
        return recorderId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
