package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.MedicationChecklistPort;
import java.time.Instant;
import java.util.*;

/** Fixture chỉ đọc của 006, khoảng thời gian nửa mở. */
public class FakeMedicationChecklist implements MedicationChecklistPort {
    private final FakeM04Switches switches;
    private final List<DoseView> doses = new ArrayList<>();

    public FakeMedicationChecklist(FakeM04Switches switches) {
        this.switches = switches;
    }

    public void add(DoseView dose) {
        doses.add(dose);
    }

    @Override
    public List<DoseView> inRange(Collection<Long> residentIds, Instant from, Instant to) {
        switches.requireEnabled("006");
        return doses.stream()
                .filter(d -> residentIds.contains(d.residentId())
                        && !d.plannedAt().isBefore(from)
                        && d.plannedAt().isBefore(to))
                .toList();
    }

    public void reset() {
        doses.clear();
    }
}
