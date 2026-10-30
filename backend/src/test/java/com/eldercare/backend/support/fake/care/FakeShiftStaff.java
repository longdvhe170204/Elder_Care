package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.common.Role;
import java.time.*;
import java.util.*;

/** Fixture 008: không tự tính điều kiện nghiệp vụ hoặc tự tạo ca/phân công. */
public class FakeShiftStaff implements ShiftStaffPort {
    private final FakeM04Switches switches;
    private final Map<Long, ShiftView> shifts = new LinkedHashMap<>();
    private final Map<Long, Long> leads = new HashMap<>();
    private final Map<Pair, List<CareAssignmentView>> assignments = new HashMap<>();
    private final Map<At, List<Long>> nurses = new HashMap<>();
    private final Map<At, Boolean> onShift = new HashMap<>();
    private final Map<Qualification, Boolean> qualifications = new HashMap<>();
    private final Map<QualifiedShift, Boolean> qualifiedShifts = new HashMap<>();
    private final Map<Long, Boolean> handovers = new HashMap<>();
    private final Map<Pair, HandoverTasksView> handoverTasks = new HashMap<>();

    public FakeShiftStaff(FakeM04Switches switches) {
        this.switches = switches;
    }

    public void shift(ShiftView shift) {
        shifts.put(shift.id(), shift);
    }

    public void shiftLead(long shiftId, long staffId) {
        leads.put(shiftId, staffId);
    }

    public void assignments(long residentId, long shiftId, List<CareAssignmentView> values) {
        assignments.put(new Pair(residentId, shiftId), List.copyOf(values));
    }

    public void responsibleNurses(long residentId, Instant at, List<Long> values) {
        nurses.put(new At(residentId, at), List.copyOf(values));
    }

    public void onShift(long staffId, Instant at, boolean value) {
        onShift.put(new At(staffId, at), value);
    }

    public void qualified(long staffId, Set<String> certificates, LocalDate date, boolean value) {
        qualifications.put(new Qualification(staffId, Set.copyOf(certificates), date), value);
    }

    public void qualifiedStaff(long shiftId, Role role, Set<String> certificates, boolean value) {
        qualifiedShifts.put(new QualifiedShift(shiftId, role, Set.copyOf(certificates)), value);
    }

    public void handoverConfirmed(long previousShiftId, boolean value) {
        handovers.put(previousShiftId, value);
    }

    public void handoverTasks(long staffId, long shiftId, HandoverTasksView value) {
        handoverTasks.put(new Pair(staffId, shiftId), value);
    }

    @Override
    public Optional<ShiftView> shift(long shiftId) {
        check();
        return Optional.ofNullable(shifts.get(shiftId));
    }

    @Override
    public List<ShiftView> publishedShifts(long floorAreaId, Instant from, Instant to) {
        check();
        return shifts.values().stream()
                .filter(s -> s.published()
                        && s.floorAreaId() == floorAreaId
                        && s.startsAt().isBefore(to)
                        && s.endsAt().isAfter(from))
                .toList();
    }

    @Override
    public Optional<ShiftView> shiftContaining(long floorAreaId, Instant at) {
        check();
        return shifts.values().stream()
                .filter(s -> s.published()
                        && s.floorAreaId() == floorAreaId
                        && !at.isBefore(s.startsAt())
                        && at.isBefore(s.endsAt()))
                .findFirst();
    }

    @Override
    public Optional<Long> shiftLead(long shiftId) {
        check();
        return Optional.ofNullable(leads.get(shiftId));
    }

    @Override
    public List<CareAssignmentView> assignments(long residentId, long shiftId) {
        check();
        return assignments.getOrDefault(new Pair(residentId, shiftId), List.of());
    }

    @Override
    public List<Long> responsibleNurses(long residentId, Instant at) {
        check();
        return nurses.getOrDefault(new At(residentId, at), List.of());
    }

    @Override
    public boolean onShift(long staffId, Instant at) {
        check();
        return onShift.getOrDefault(new At(staffId, at), false);
    }

    @Override
    public boolean qualified(long staffId, Set<String> certificates, LocalDate date) {
        check();
        return qualifications.getOrDefault(new Qualification(staffId, certificates, date), false);
    }

    @Override
    public boolean hasQualifiedStaff(long shiftId, Role role, Set<String> certificates) {
        check();
        return qualifiedShifts.getOrDefault(new QualifiedShift(shiftId, role, certificates), false);
    }

    @Override
    public boolean handoverConfirmed(long previousShiftId) {
        check();
        return handovers.getOrDefault(previousShiftId, false);
    }

    @Override
    public HandoverTasksView handoverTasks(long staffId, long shiftId) {
        check();
        return handoverTasks.getOrDefault(new Pair(staffId, shiftId), new HandoverTasksView(false, false));
    }

    public void reset() {
        shifts.clear();
        leads.clear();
        assignments.clear();
        nurses.clear();
        onShift.clear();
        qualifications.clear();
        qualifiedShifts.clear();
        handovers.clear();
        handoverTasks.clear();
    }

    private void check() {
        switches.requireEnabled("008");
    }

    private record Pair(long first, long second) {}

    private record At(long id, Instant at) {}

    private record Qualification(long staffId, Set<String> certificates, LocalDate date) {}

    private record QualifiedShift(long shiftId, Role role, Set<String> certificates) {}
}
