package com.eldercare.backend.medication.internal.family.domain;

import java.time.Instant;

public class FamilyMedicationPrescription {

    private Long id;

    private Long batchId;

    private Long prescriptionId;

    private Instant boundFrom;

    private Instant boundTo;

    public FamilyMedicationPrescription() {}

    public FamilyMedicationPrescription(Long batchId, Long prescriptionId, Instant boundFrom) {
        this.batchId = batchId;
        this.prescriptionId = prescriptionId;
        this.boundFrom = boundFrom;
    }

    public void unbind(Instant boundTo) {
        this.boundTo = boundTo;
    }

    public void assignId(Long id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public Instant getBoundFrom() {
        return boundFrom;
    }

    public Instant getBoundTo() {
        return boundTo;
    }
}
