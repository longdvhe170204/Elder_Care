package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import java.time.LocalDate;
import java.util.*;

/** Fixture riêng của từng nguồn 006/012/014; không sinh mục thời khóa biểu. */
public class FakeTimetableSource implements TimetableSourcePort {
    private final FakeM04Switches switches;
    private final String module;
    private final Map<ResidentDate, List<TimetableItemView>> items = new HashMap<>();

    public FakeTimetableSource(FakeM04Switches switches, String module) {
        this.switches = switches;
        this.module = module;
    }

    public void items(long residentId, LocalDate date, List<TimetableItemView> values) {
        items.put(new ResidentDate(residentId, date), List.copyOf(values));
    }

    @Override
    public List<TimetableItemView> onDate(long residentId, LocalDate date) {
        switches.requireEnabled(module);
        return items.getOrDefault(new ResidentDate(residentId, date), List.of());
    }

    public void reset() {
        items.clear();
    }

    private record ResidentDate(long residentId, LocalDate date) {}
}
