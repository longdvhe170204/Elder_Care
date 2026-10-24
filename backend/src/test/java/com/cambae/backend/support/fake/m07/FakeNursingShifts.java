package com.cambae.backend.support.fake.m07;

import com.cambae.backend.m07.api.dto.NursingShiftView;
import com.cambae.backend.m07.api.spi.NursingShiftPort;
import java.time.Instant;
import java.util.*;

public class FakeNursingShifts implements NursingShiftPort {
    private record Entry(NursingShiftView view, List<Long> nurses) {}

    private final Map<Long, Entry> shifts = new LinkedHashMap<>();

    private record Assignment(long resident, long staff, Instant from, Instant to) {}

    private final List<Assignment> assignments = new ArrayList<>();

    public FakeNursingShifts assign(long resident, long staff, Instant from, Instant to) {
        assignments.add(new Assignment(resident, staff, from, to));
        return this;
    }

    public Optional<Long> assignedTo(long resident, Instant at) {
        return assignments.stream()
                .filter(a -> a.resident() == resident && !a.from().isAfter(at) && a.to().isAfter(at))
                .map(Assignment::staff)
                .findFirst();
    }

    public FakeNursingShifts shift(NursingShiftView view, List<Long> nurses) {
        shifts.put(view.id(), new Entry(view, List.copyOf(nurses)));
        return this;
    }

    private boolean at(Entry entry, Instant at) {
        return !entry.view().startsAt().isAfter(at) && entry.view().endsAt().isAfter(at);
    }

    public Optional<NursingShiftView> shiftAt(long floor, Instant at) {
        return shifts.values().stream()
                .filter(e -> Objects.equals(e.view().floorAreaId(), floor) && at(e, at))
                .map(Entry::view)
                .findFirst();
    }

    public List<Long> nursesWithShift(long floor, Instant at) {
        return shifts.values().stream()
                .filter(e -> Objects.equals(e.view().floorAreaId(), floor) && at(e, at))
                .flatMap(e -> e.nurses().stream())
                .distinct()
                .toList();
    }

    public boolean inProgressShiftMember(long staff, Instant at) {
        return shifts.values().stream().anyMatch(e -> at(e, at) && e.nurses().contains(staff));
    }
}
