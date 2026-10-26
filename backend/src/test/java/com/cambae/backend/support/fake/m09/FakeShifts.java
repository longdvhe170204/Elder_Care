package com.cambae.backend.support.fake.m09;

import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m09.api.dto.ShiftView;
import com.cambae.backend.m09.api.dto.StaffPublicView;
import java.time.Instant;
import java.util.*;

/** Bản giả tra cứu ca do 008 cung cấp; test tự đặt ca và nhân viên, không lập ca thật. */
public class FakeShifts implements ShiftApi {
    private record Entry(ShiftView view, List<Long> staffIds) {}

    private final Map<Long, Entry> shifts = new LinkedHashMap<>();
    private final Map<Long, StaffPublicView> staff = new LinkedHashMap<>();
    private final Map<Long, Set<Long>> doctors = new LinkedHashMap<>();

    /** Xóa ca, nhân viên, Bác sĩ trực đã đặt (bản giả dùng chung cả context test). */
    public void reset() {
        shifts.clear();
        staff.clear();
        doctors.clear();
    }

    public FakeShifts shift(ShiftView view, List<Long> staffIds) {
        shifts.put(view.id(), new Entry(view, List.copyOf(staffIds)));
        return this;
    }

    public FakeShifts staff(StaffPublicView view) {
        staff.put(view.id(), view);
        return this;
    }

    public Optional<ShiftView> currentShift(long floorAreaId, Instant at) {
        return shifts.values().stream()
                .map(Entry::view)
                .filter(v -> Objects.equals(v.floorAreaId(), floorAreaId))
                .filter(v -> "IN_PROGRESS".equals(v.status()) && !at.isBefore(v.startsAt()) && at.isBefore(v.endsAt()))
                .findFirst();
    }

    public List<ShiftView> staffShifts(long staffId, Instant from, Instant to) {
        return shifts.values().stream()
                .filter(e -> e.staffIds().contains(staffId))
                .map(Entry::view)
                .filter(v -> v.startsAt().isBefore(to) && v.endsAt().isAfter(from))
                .toList();
    }

    public boolean onShift(long staffId, Instant at) {
        return shifts.values().stream()
                .filter(e -> e.staffIds().contains(staffId))
                .map(Entry::view)
                .anyMatch(
                        v -> "IN_PROGRESS".equals(v.status()) && !at.isBefore(v.startsAt()) && at.isBefore(v.endsAt()));
    }

    public Optional<StaffPublicView> staff(long staffId) {
        return Optional.ofNullable(staff.get(staffId));
    }

    /** Đánh dấu Bác sĩ trực của một ca đã đặt; chỉ tính khi ca đang diễn ra. */
    public FakeShifts onDutyDoctor(long shiftId, long staffId) {
        doctors.computeIfAbsent(shiftId, k -> new LinkedHashSet<>()).add(staffId);
        return this;
    }

    public List<Long> staffOnShift(long floorAreaId, Instant at) {
        return shifts.values().stream()
                .filter(e -> Objects.equals(e.view().floorAreaId(), floorAreaId) && inProgress(e.view(), at))
                .flatMap(e -> e.staffIds().stream())
                .distinct()
                .toList();
    }

    public List<Long> onDutyDoctors(Instant at) {
        return shifts.values().stream()
                .filter(e -> inProgress(e.view(), at))
                .flatMap(e -> doctors.getOrDefault(e.view().id(), Set.of()).stream())
                .distinct()
                .toList();
    }

    private static boolean inProgress(ShiftView v, Instant at) {
        return "IN_PROGRESS".equals(v.status()) && !at.isBefore(v.startsAt()) && at.isBefore(v.endsAt());
    }
}
