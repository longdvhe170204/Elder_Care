package com.eldercare.backend.medication.internal.prescription.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Dòng {@code record_event} loại {@code M07.PRESCRIPTION_BASIS}, đối tượng {@code M07.PRESCRIPTION} (006 T069, {@code V006_98}). */
public class PrescriptionBasis {

    private Long id;

    private Long prescriptionId;

    private PrescriptionBasisCommand command;

    private String facilityName;

    private String prescriberName;

    private LocalDate prescribedDate;

    private PrescriptionBasisType basisType;

    private String callerName;

    private String receiverName;

    private Instant calledAt;

    private Long documentFileId;

    private Long recorderId;

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
            Instant recordedAt,
            Instant now) {
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
        this.recordedAt = recordedAt != null ? recordedAt : now;
    }

    public void assignId(long id) {
        this.id = id;
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
