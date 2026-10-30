package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.dto.AlertShiftView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import java.time.Instant;
import java.util.*;

public class FakeAlertStaff implements AlertStaffPort {
    private final Map<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView, Instant> awaitingHandovers =
            new HashMap<>();

    public FakeAlertStaff awaitingHandover(
            com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView row, Instant startsAt) {
        awaitingHandovers.put(row, startsAt);
        return this;
    }

    public List<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView> startedWithoutConfirmedHandover(
            Instant from, Instant to) {
        return awaitingHandovers.entrySet().stream()
                .filter(e -> e.getValue().isAfter(from) && !e.getValue().isAfter(to))
                .map(Map.Entry::getKey)
                .toList();
    }

    private record Entry(AlertShiftView view, List<Long> staff, List<Long> doctors) {}

    private final Map<Long, Entry> shifts = new LinkedHashMap<>();
    private final Map<Long, Long> managers = new HashMap<>();
    private final Map<Long, List<com.eldercare.backend.incident.api.dto.AssignmentTraceView>> assignments =
            new HashMap<>();
    private final Map<Long, Long> nurses = new HashMap<>();

    public FakeAlertStaff assignments(
            long resident, List<com.eldercare.backend.incident.api.dto.AssignmentTraceView> rows) {
        assignments.put(resident, List.copyOf(rows));
        return this;
    }

    public FakeAlertStaff nurse(long resident, long staff) {
        nurses.put(resident, staff);
        return this;
    }

    public Optional<Long> assignedNurse(long resident, Instant at) {
        return Optional.ofNullable(nurses.get(resident)).filter(staff -> inProgressShiftMember(staff, at));
    }

    public List<com.eldercare.backend.incident.api.dto.AssignmentTraceView> assignmentsInRange(
            long resident, Instant from, Instant to) {
        return assignments.getOrDefault(resident, List.of()).stream()
                .filter(a -> a.from().isBefore(to) && a.to().isAfter(from))
                .toList();
    }

    public FakeAlertStaff shift(AlertShiftView view, List<Long> staff, List<Long> doctors) {
        shifts.put(view.id(), new Entry(view, List.copyOf(staff), List.copyOf(doctors)));
        return this;
    }

    public FakeAlertStaff floorManager(long floor, long staff) {
        managers.put(floor, staff);
        return this;
    }

    private boolean at(Entry entry, Instant at) {
        return !entry.view().startsAt().isAfter(at) && entry.view().endsAt().isAfter(at);
    }

    public Optional<AlertShiftView> shiftAt(long floor, Instant at) {
        return shifts.values().stream()
                .filter(e -> Objects.equals(e.view().floorAreaId(), floor) && at(e, at))
                .map(Entry::view)
                .findFirst();
    }

    public Optional<AlertShiftView> nextShift(long floor, Instant at) {
        return shifts.values().stream()
                .map(Entry::view)
                .filter(s ->
                        Objects.equals(s.floorAreaId(), floor) && s.startsAt().isAfter(at))
                .min(Comparator.comparing(AlertShiftView::startsAt));
    }

    public List<Long> onDutyDoctors(Instant at) {
        return shifts.values().stream()
                .filter(e -> at(e, at))
                .flatMap(e -> e.doctors().stream())
                .distinct()
                .toList();
    }

    public Optional<Long> assignedFloorManager(long floor, Instant at) {
        return Optional.ofNullable(managers.get(floor));
    }

    public boolean listedInShift(long staff, long shift) {
        return shifts.containsKey(shift) && shifts.get(shift).staff().contains(staff);
    }

    public boolean inProgressShiftMember(long staff, Instant at) {
        return shifts.values().stream().anyMatch(e -> at(e, at) && e.staff().contains(staff));
    }

    public boolean onShift(long staff, Instant at) {
        return inProgressShiftMember(staff, at);
    }

    public Optional<Long> floorDuringShift(long staff, Instant at) {
        return shifts.values().stream()
                .filter(e -> e.staff().contains(staff) && at(e, at))
                .map(e -> e.view().floorAreaId())
                .filter(Objects::nonNull)
                .findFirst();
    }

    public List<Long> floorsWithShift(Instant at) {
        return shifts.values().stream()
                .filter(e -> at(e, at))
                .map(e -> e.view().floorAreaId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
