package com.cambae.backend.m07.internal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "supplied_medication_intake")
public class SuppliedMedicationIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "procurement_line_id", nullable = false)
    private Long procurementLineId;

    @Column(name = "procurement_request_id", nullable = false)
    private Long procurementRequestId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "quantity_received", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityReceived;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_result", nullable = false)
    private IntakeResult intakeResult;

    @Column(name = "discrepancy", length = 2000)
    private String discrepancy;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum IntakeResult {
        PASS,
        FAIL
    }

    public SuppliedMedicationIntake() {}

    public SuppliedMedicationIntake(
            Long procurementLineId,
            Long procurementRequestId,
            Long residentId,
            Long prescriptionId,
            String medicationName,
            BigDecimal quantityReceived,
            String unit,
            IntakeResult intakeResult,
            String discrepancy,
            String reason,
            Long receiverId,
            Instant receivedAt) {
        this.procurementLineId = procurementLineId;
        this.procurementRequestId = procurementRequestId;
        this.residentId = residentId;
        this.prescriptionId = prescriptionId;
        this.medicationName = medicationName;
        this.quantityReceived = quantityReceived;
        this.unit = unit;
        this.intakeResult = intakeResult;
        this.discrepancy = discrepancy;
        this.reason = reason;
        this.receiverId = receiverId;
        this.receivedAt = receivedAt;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProcurementLineId() {
        return procurementLineId;
    }

    public Long getProcurementRequestId() {
        return procurementRequestId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public BigDecimal getQuantityReceived() {
        return quantityReceived;
    }

    public String getUnit() {
        return unit;
    }

    public IntakeResult getIntakeResult() {
        return intakeResult;
    }

    public String getDiscrepancy() {
        return discrepancy;
    }

    public String getReason() {
        return reason;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
