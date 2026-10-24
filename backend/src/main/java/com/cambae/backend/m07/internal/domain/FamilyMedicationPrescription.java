package com.cambae.backend.m07.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "family_medication_prescription")
public class FamilyMedicationPrescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "bound_from", nullable = false)
    private Instant boundFrom;

    @Column(name = "bound_to")
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
