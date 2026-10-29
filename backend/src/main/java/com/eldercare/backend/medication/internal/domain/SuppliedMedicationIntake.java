package com.eldercare.backend.medication.internal.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Lần tiếp nhận thuốc cung ứng (data-model mục 9a, FR-054): nhóm 3, chỉ thêm; sai sót sửa bằng đính chính. */
@Entity
@DataGroup(DataGroup.Group.RECORD)
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

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "concentration", nullable = false, length = 100)
    private String concentration;

    @Column(name = "dosage_form", nullable = false, length = 100)
    private String dosageForm;

    @Column(name = "quantity_received", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityReceived;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "item_condition", nullable = false, length = 500)
    private String condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_result", nullable = false)
    private IntakeResult intakeResult;

    @Column(name = "discrepancy", length = 2000)
    private String discrepancy;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "deliverer_name", nullable = false)
    private String delivererName;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum IntakeResult {
        PASSED,
        NOT_PASSED
    }

    protected SuppliedMedicationIntake() {}

    public SuppliedMedicationIntake(
            Long procurementLineId,
            Long procurementRequestId,
            Long residentId,
            Long prescriptionId,
            Long medicationId,
            String medicationName,
            String concentration,
            String dosageForm,
            BigDecimal quantityReceived,
            String unit,
            LocalDate expiryDate,
            String condition,
            IntakeResult intakeResult,
            String discrepancy,
            String reason,
            String delivererName,
            Long receiverId,
            Instant now) {
        this.procurementLineId = procurementLineId;
        this.procurementRequestId = procurementRequestId;
        this.residentId = residentId;
        this.prescriptionId = prescriptionId;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.concentration = concentration;
        this.dosageForm = dosageForm;
        this.quantityReceived = quantityReceived;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.condition = condition;
        this.intakeResult = intakeResult;
        this.discrepancy = discrepancy;
        this.reason = reason;
        this.delivererName = delivererName;
        this.receiverId = receiverId;
        this.receivedAt = now;
        this.createdAt = now;
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

    public Long getMedicationId() {
        return medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getConcentration() {
        return concentration;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public BigDecimal getQuantityReceived() {
        return quantityReceived;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getCondition() {
        return condition;
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

    public String getDelivererName() {
        return delivererName;
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
