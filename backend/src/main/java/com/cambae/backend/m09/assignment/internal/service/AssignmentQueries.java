package com.cambae.backend.m09.assignment.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.m09.assignment.internal.domain.*;
import com.cambae.backend.m09.assignment.internal.domain.PrimaryAssigneeResolver.TargetRef;
import com.cambae.backend.m09.assignment.internal.persistence.AssignmentStore;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.spi.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T021: phân công thật; thành viên ca không tự có phạm vi cả tầng khi chưa được phân công. */
@Service
@Transactional(readOnly = true)
public class AssignmentQueries implements AssignmentApi {
    private final AssignmentStore store;
    private final ShiftStore shifts;
    private final AssignmentSources sources;

    public AssignmentQueries(AssignmentStore store, ShiftStore shifts, AssignmentSources sources) {
        this.store = store;
        this.shifts = shifts;
        this.sources = sources;
    }

    public List<AssignmentView> history(long staff, Instant from, Instant to) {
        var views = new ArrayList<AssignmentView>();
        for (var a : store.history(staff, from, to)) {
            var s = shifts.shift(a.shiftId(), false);
            Instant cut =
                    a.actualEndAt() != null ? a.actualEndAt() : a.endsAt().isBefore(s.endsAt()) ? a.endsAt() : null;
            Long floor = Set.of("FLOOR", "AREA").contains(a.targetType()) ? a.targetId() : null,
                    room = "ROOM".equals(a.targetType()) ? a.targetId() : null,
                    resident = "RESIDENT".equals(a.targetType()) ? a.targetId() : null;
            views.add(new AssignmentView(
                    AssignmentKind.SHIFT_MEMBER, floor, room, resident, s.id(), s.startsAt(), s.endsAt(), cut));
        }
        for (var a : store.managersForStaff(staff, from, to))
            views.add(new AssignmentView(
                    AssignmentKind.FLOOR_MANAGER, a.floorAreaId(), null, null, null, a.startsAt(), a.endsAt()));
        // Chỉ người phụ trách có phạm vi toàn ca, lấy lịch sử đã được công bố.
        for (var s : shifts.staffRange(staff, from, to, true)) {
            if (Objects.equals(s.leadStaffId(), staff) && !s.missingLead() && s.scopeId() != null)
                views.add(new AssignmentView(
                        AssignmentKind.SHIFT_LEAD, s.scopeId(), null, null, s.id(), s.startsAt(), s.endsAt()));
        }
        return List.copyOf(views);
    }

    public List<AssignmentView> activeAt(long staff, Instant at) {
        return history(staff, at, at.plusNanos(1000)).stream()
                .filter(a -> !a.from().isAfter(at)
                        && (a.to() == null || a.to().isAfter(at))
                        && (a.scopeEndsAt() == null || a.scopeEndsAt().isAfter(at)))
                .toList();
    }

    public List<Long> staffAssignedToResident(long resident, Instant at) {
        return atResident(resident, at).stream()
                .map(StaffAssignment::staffId)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Long> staffAssignedToRoom(long room, Instant at) {
        var r = sources.structure().room(room);
        if (r.isEmpty()) return List.of();
        Long area = sources.structure()
                .floor(r.get().floorAreaId())
                .map(f -> f.areaId())
                .orElse(null);
        var refs = new HashSet<TargetRef>();
        refs.add(new TargetRef("ROOM", room));
        refs.add(new TargetRef("FLOOR", r.get().floorAreaId()));
        if (area != null) refs.add(new TargetRef("AREA", area));
        return store.atTargets(refs, at).stream()
                .filter(a -> present(a, at))
                .map(StaffAssignment::staffId)
                .distinct()
                .sorted()
                .toList();
    }

    public List<StaffAssignment> atResident(long resident, Instant at) {
        return store.atTargets(positionRefs(resident, at), at).stream()
                .filter(a -> present(a, at))
                .toList();
    }

    public List<StaffAssignment> inResidentShift(long resident, long shift, Instant at) {
        return store.inShift(shift, false).stream()
                .filter(a -> !Set.of("DRAFT", "CANCELLED").contains(a.status())
                        && a.activeAt(at)
                        && sources.matches(a, resident, at)
                        && present(a, at))
                .toList();
    }

    private boolean present(StaffAssignment a, Instant at) {
        return shifts.members(a.shiftId()).stream().anyMatch(m -> m.staffId() == a.staffId() && m.presentAt(at));
    }

    public Optional<Long> primary(long resident, Role role, Long task, Instant at, boolean nurseOnly) {
        var rows = atResident(resident, at).stream()
                .filter(a -> !nurseOnly || a.assignedNurse())
                .toList();
        var refs = new HashSet<TargetRef>();
        refs.add(new TargetRef("RESIDENT", resident));
        sources.position(resident, at).ifPresent(p -> {
            refs.add(new TargetRef("ROOM", p.roomId()));
            if (p.floorAreaId() != null) refs.add(new TargetRef("FLOOR", p.floorAreaId()));
            if (p.areaId() != null) refs.add(new TargetRef("AREA", p.areaId()));
        });
        return PrimaryAssigneeResolver.resolve(rows, refs, role, task, at).map(StaffAssignment::staffId);
    }

    public List<Long> assignedNurses(long resident, Instant at) {
        return primary(resident, Role.NURSING_STAFF, null, at, true).stream().toList();
    }

    public List<com.cambae.backend.m04.api.dto.CareAssignmentView> careAssignments(long resident, long shift) {
        var s = shifts.shift(shift, false);
        if (Set.of("DRAFT", "CANCELLED").contains(s.status())) return List.of();
        var rows = inResidentShift(resident, shift, s.startsAt());
        var refs = positionRefs(resident, s.startsAt());
        var selected = new LinkedHashSet<StaffAssignment>();
        rows.stream().filter(a -> "SUPPORT".equals(a.assignmentKind())).forEach(selected::add);
        for (var role : Role.values()) {
            PrimaryAssigneeResolver.resolve(rows, refs, role, null, s.startsAt())
                    .ifPresent(selected::add);
            for (long task : rows.stream()
                    .map(StaffAssignment::taskTypeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList())
                PrimaryAssigneeResolver.resolve(rows, refs, role, task, s.startsAt())
                        .ifPresent(selected::add);
        }
        return selected.stream()
                .map(a -> new com.cambae.backend.m04.api.dto.CareAssignmentView(
                        a.id(), a.staffId(), a.role(), a.assignmentKind(), a.taskTypeId(), a.assignedNurse()))
                .toList();
    }

    private Set<TargetRef> positionRefs(long resident, Instant at) {
        var refs = new HashSet<TargetRef>();
        refs.add(new TargetRef("RESIDENT", resident));
        sources.position(resident, at).ifPresent(p -> {
            refs.add(new TargetRef("ROOM", p.roomId()));
            if (p.floorAreaId() != null) refs.add(new TargetRef("FLOOR", p.floorAreaId()));
            if (p.areaId() != null) refs.add(new TargetRef("AREA", p.areaId()));
        });
        return refs;
    }

    public List<com.cambae.backend.m05.api.dto.AssignmentTraceView> trace(long resident, Instant from, Instant to) {
        if (!from.isBefore(to)) return List.of();
        var result = new ArrayList<com.cambae.backend.m05.api.dto.AssignmentTraceView>();
        var history = sources.stays(resident, from, to);
        var refs = new HashSet<TargetRef>();
        refs.add(new TargetRef("RESIDENT", resident));
        for (var stay : history) {
            var p = stay.location();
            refs.add(new TargetRef("ROOM", p.roomId()));
            if (p.floorAreaId() != null) refs.add(new TargetRef("FLOOR", p.floorAreaId()));
            if (p.areaId() != null) refs.add(new TargetRef("AREA", p.areaId()));
        }
        for (var a : store.effectiveTargets(refs, from, to)) {
            if ("RESIDENT".equals(a.targetType())) {
                if (a.targetId() == resident) addTrace(result, a, from, to);
                continue;
            }
            for (var stay : history)
                if (AssignmentSources.matches(a.targetType(), a.targetId(), stay.location()))
                    addTrace(
                            result,
                            a,
                            stay.from().isAfter(from) ? stay.from() : from,
                            stay.to() != null && stay.to().isBefore(to) ? stay.to() : to);
        }
        return result.stream()
                .distinct()
                .sorted(Comparator.comparing(com.cambae.backend.m05.api.dto.AssignmentTraceView::from)
                        .thenComparingLong(com.cambae.backend.m05.api.dto.AssignmentTraceView::assignmentId))
                .toList();
    }

    private static void addTrace(
            List<com.cambae.backend.m05.api.dto.AssignmentTraceView> rows,
            StaffAssignment a,
            Instant from,
            Instant to) {
        Instant start = a.startsAt().isAfter(from) ? a.startsAt() : from,
                end = a.effectiveEnd().isBefore(to) ? a.effectiveEnd() : to;
        if (start.isBefore(end))
            rows.add(new com.cambae.backend.m05.api.dto.AssignmentTraceView(
                    a.id(), a.staffId(), a.role(), a.assignmentKind(), a.targetType(), a.targetId(), start, end));
    }

    public Optional<Long> floorManager(long floor, Instant at) {
        var found = store.managers(floor, false).stream()
                .filter(a -> !a.startsAt().isAfter(at)
                        && (a.endsAt() == null || a.endsAt().isAfter(at)))
                .toList();
        if (found.size() > 1)
            throw new InvalidPermissionDataException("M09.FLOOR_MANAGER_ASSIGNMENT", "MULTIPLE_FLOOR_MANAGERS", floor);
        return found.stream().map(FloorManagerAssignment::staffId).findFirst();
    }

    public List<Long> floorReceivers(long floor, long shift) {
        return store.inShift(shift, false).stream()
                .filter(a -> !Set.of("DRAFT", "CANCELLED", "ENDED").contains(a.status())
                        && Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER)
                                .contains(a.role())
                        && sources.floors(a.targetType(), a.targetId(), a.startsAt())
                                .contains(floor))
                .map(StaffAssignment::staffId)
                .distinct()
                .sorted()
                .toList();
    }
}
