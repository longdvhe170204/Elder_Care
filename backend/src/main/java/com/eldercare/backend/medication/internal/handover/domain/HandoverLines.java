package com.eldercare.backend.medication.internal.handover.domain;

import java.util.ArrayList;
import java.util.List;

/** Cột {@code medication_handover.lines} của lần giao: dòng thuốc giao và liều mang theo (006 T069, {@code V006_97}). */
public class HandoverLines {

    private List<MedicationHandoverItem> items = new ArrayList<>();

    private List<MedicationHandoverDose> doses = new ArrayList<>();

    public HandoverLines() {}

    public HandoverLines(List<MedicationHandoverItem> items, List<MedicationHandoverDose> doses) {
        this.items = new ArrayList<>(items == null ? List.of() : items);
        this.doses = new ArrayList<>(doses == null ? List.of() : doses);
    }

    public List<MedicationHandoverItem> getItems() {
        return items == null ? List.of() : List.copyOf(items);
    }

    public List<MedicationHandoverDose> getDoses() {
        return doses == null ? List.of() : List.copyOf(doses);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HandoverLines other
                && java.util.Objects.equals(items, other.items)
                && java.util.Objects.equals(doses, other.doses);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(items, doses);
    }
}
