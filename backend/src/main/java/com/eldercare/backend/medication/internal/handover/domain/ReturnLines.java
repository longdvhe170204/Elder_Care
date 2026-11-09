package com.eldercare.backend.medication.internal.handover.domain;

import java.util.ArrayList;
import java.util.List;

/** Cột {@code medication_handover.lines} của lần nhận lại: dòng thuốc nhận lại (006 T069, {@code V006_97}). */
public class ReturnLines {

    private List<MedicationReturnItem> items = new ArrayList<>();

    public ReturnLines() {}

    public ReturnLines(List<MedicationReturnItem> items) {
        this.items = new ArrayList<>(items == null ? List.of() : items);
    }

    public List<MedicationReturnItem> getItems() {
        return items == null ? List.of() : List.copyOf(items);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReturnLines other && java.util.Objects.equals(items, other.items);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(items);
    }
}
