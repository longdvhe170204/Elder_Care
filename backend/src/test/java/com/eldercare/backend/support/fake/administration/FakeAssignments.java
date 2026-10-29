package com.eldercare.backend.support.fake.administration;

import com.eldercare.backend.administration.account.api.spi.AssignmentApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
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
    public List<Long> staffAssignedToResident(long residentId, Instant at) {
        return assignedAt(at, a -> Long.valueOf(residentId).equals(a.residentId()));
    }

    @Override
    public List<Long> staffAssignedToRoom(long roomId, Instant at) {
        return assignedAt(at, a -> Long.valueOf(roomId).equals(a.roomId()));
    }

    // Bản giả chỉ xét phân công gắn thẳng đối tượng; bản thật của 008 tính cả phòng, tầng người cao tuổi đang ở
    private List<Long> assignedAt(Instant at, java.util.function.Predicate<AssignmentView> target) {
        return assignments.stream()
                .filter(e -> target.test(e.assignment()))
                .filter(e -> !e.assignment().from().isAfter(at)
                        && (e.assignment().to() == null || e.assignment().to().isAfter(at))
                        && (e.assignment().scopeEndsAt() == null
                                || e.assignment().scopeEndsAt().isAfter(at)))
                .map(Entry::staffId)
                .distinct()
                .toList();
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
                .filter(a -> a.scopeEndsAt() == null || a.scopeEndsAt().isAfter(from))
                .toList();
    }
}
