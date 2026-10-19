package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.AssignmentApi;
import com.cambae.backend.m15.account.api.AssignmentView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Bản giả {@link AssignmentApi} (008 cài): lọc phân công test đặt sẵn theo khoảng {@code [from, to)}. */
public class FakeAssignments implements AssignmentApi {

    private record Entry(long staffId, AssignmentView assignment) {}

    private final List<Entry> assignments = new ArrayList<>();

    public FakeAssignments assign(long staffId, AssignmentView assignment) {
        assignments.add(new Entry(staffId, assignment));
        return this;
    }

    @Override
    public List<AssignmentView> activeAt(long staffId, Instant at) {
        return history(staffId, at, at.plusNanos(1));
    }

    @Override
    public List<AssignmentView> history(long staffId, Instant from, Instant to) {
        return assignments.stream()
                .filter(e -> e.staffId() == staffId)
                .map(Entry::assignment)
                .filter(a -> a.from().isBefore(to) && (a.to() == null || a.to().isAfter(from)))
                .toList();
    }
}
