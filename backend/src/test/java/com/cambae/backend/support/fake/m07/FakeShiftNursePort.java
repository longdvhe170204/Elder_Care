package com.cambae.backend.support.fake.m07;

import com.cambae.backend.m07.api.spi.ShiftNursePort;
import com.cambae.backend.m07.api.spi.ShiftView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bản giả {@link ShiftNursePort} (Module 008 cài): trả về dữ liệu ca trực và điều dưỡng phục vụ test.
 */
public class FakeShiftNursePort implements ShiftNursePort {

    private final Map<Long, ShiftView> floorShifts = new HashMap<>();
    private final Map<Long, Long> residentNurses = new HashMap<>();
    private final Map<Long, List<Long>> floorNurses = new HashMap<>();
    private final Set<Long> activeStaff = new HashSet<>();
    private final Set<Long> confirmedShifts = new HashSet<>();

    public FakeShiftNursePort shift(long floorAreaId, ShiftView shift) {
        floorShifts.put(floorAreaId, shift);
        return this;
    }

    public FakeShiftNursePort assignNurse(long residentId, long nurseStaffId) {
        residentNurses.put(residentId, nurseStaffId);
        return this;
    }

    public FakeShiftNursePort nursesOnShift(long floorAreaId, List<Long> staffIds) {
        floorNurses.put(floorAreaId, new ArrayList<>(staffIds));
        return this;
    }

    public FakeShiftNursePort activeStaff(long staffId) {
        activeStaff.add(staffId);
        return this;
    }

    public FakeShiftNursePort confirmHandover(long shiftId) {
        confirmedShifts.add(shiftId);
        return this;
    }

    @Override
    public Optional<ShiftView> shiftContaining(long floorAreaId, Instant time) {
        return Optional.ofNullable(floorShifts.get(floorAreaId));
    }

    @Override
    public Optional<Long> assignedNurse(long residentId, Instant time) {
        return Optional.ofNullable(residentNurses.get(residentId));
    }

    @Override
    public List<Long> nursesOnShift(long floorAreaId, Instant time) {
        return floorNurses.getOrDefault(floorAreaId, List.of());
    }

    @Override
    public boolean isStaffOnActiveShift(long staffId, Instant time) {
        return activeStaff.contains(staffId);
    }

    @Override
    public boolean isHandoverConfirmed(long shiftId) {
        return confirmedShifts.contains(shiftId);
    }
}
