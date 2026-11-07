package com.eldercare.backend.medication.internal.handover.domain;

import java.time.Instant;

/** Liều mang theo của lần giao, nhúng trong {@code medication_handover.lines.doses} (006 T069, {@code V006_97}); một liều thuộc tối đa một lần giao, kiểm ở domain. */
public class MedicationHandoverDose {

    private Long id;

    private Long handoverId;

    private Long doseId;

    private Instant revertedAt;

    public MedicationHandoverDose() {}

    public MedicationHandoverDose(Long handoverId, Long doseId) {
        this.handoverId = handoverId;
        this.doseId = doseId;
    }

    public void markReverted(Instant revertedAt) {
        this.revertedAt = revertedAt;
    }

    public void assignId(Long id) {
        if (this.id == null) {
            this.id = id;
        }
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

    @Override
    public boolean equals(Object o) {
        return o instanceof MedicationHandoverDose other
                && java.util.Objects.equals(id, other.id)
                && java.util.Objects.equals(handoverId, other.handoverId)
                && java.util.Objects.equals(doseId, other.doseId)
                && java.util.Objects.equals(revertedAt, other.revertedAt);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, handoverId, doseId, revertedAt);
    }
}
