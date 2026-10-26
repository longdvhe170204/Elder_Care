package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Carried dose link to handover record (FR-048a).
 */
@Entity
@DataGroup(DataGroup.Group.RECORD)
@Table(name = "medication_handover_dose")
public class MedicationHandoverDose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_id", nullable = false)
    private Long handoverId;

    @Column(name = "dose_id", nullable = false, unique = true)
    private Long doseId;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    protected MedicationHandoverDose() {}

    public MedicationHandoverDose(Long handoverId, Long doseId) {
        this.handoverId = handoverId;
        this.doseId = doseId;
    }

    public void markReverted(Instant revertedAt) {
        this.revertedAt = revertedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getHandoverId() {
        return handoverId;
    }

    public Long getDoseId() {
        return doseId;
    }

    public Instant getRevertedAt() {
        return revertedAt;
    }
}
