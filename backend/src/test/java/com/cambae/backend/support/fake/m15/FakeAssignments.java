package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.AssignmentApi;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.InvalidPermissionDataException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bản giả {@link AssignmentApi} (008 cài): lọc phân công test đặt sẵn theo khoảng {@code [from, to)}. */
public class FakeAssignments implements AssignmentApi {

    private record Entry(long staffId, AssignmentView assignment) {}

    private final List<Entry> assignments = new ArrayList<>();
    private final Map<Long, InvalidPermissionDataException> invalid = new HashMap<>();

    public FakeAssignments assign(long staffId, AssignmentView assignment) {
        assignments.add(new Entry(staffId, assignment));
        return this;
    }

    /** Kết thúc mọi phân công của nhân viên (đổi phân công, 002 US4 #4). */
    public FakeAssignments unassign(long staffId) {
        assignments.removeIf(e -> e.staffId() == staffId);
        invalid.remove(staffId);
        return this;
    }

    /** Dữ liệu nguồn lỗi: phân công của nhân viên này ném lỗi (002 FR-041b, US4 #13). */
    public FakeAssignments invalid(long staffId, String errorKind) {
        invalid.put(staffId, new InvalidPermissionDataException("M09.ASSIGNMENT", errorKind, staffId));
        return this;
    }

    @Override
    public List<AssignmentView> activeAt(long staffId, Instant at) {
        return history(staffId, at, at.plusNanos(1));
    }

    @Override
    public List<AssignmentView> history(long staffId, Instant from, Instant to) {
        if (invalid.containsKey(staffId)) {
            throw invalid.get(staffId);
        }
        return assignments.stream()
                .filter(e -> e.staffId() == staffId)
                .map(Entry::assignment)
                .filter(a -> a.from().isBefore(to) && (a.to() == null || a.to().isAfter(from)))
                .toList();
    }
}
