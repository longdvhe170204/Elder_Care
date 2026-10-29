package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.medication.api.spi.ShiftNursePort;
import com.eldercare.backend.medication.api.spi.ShiftView;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Fallback implementation of {@link ShiftNursePort} when M08 is not present.
 */
public class DefaultShiftNursePort implements ShiftNursePort {

    @Override
    public Optional<ShiftView> shiftContaining(long floorAreaId, Instant time) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> assignedNurse(long residentId, Instant time) {
        return Optional.empty();
    }

    @Override
    public List<Long> nursesOnShift(long floorAreaId, Instant time) {
        return Collections.emptyList();
    }

    @Override
    public boolean isStaffOnActiveShift(long staffId, Instant time) {
        return true;
    }

    @Override
    public boolean isHandoverConfirmed(long shiftId) {
        return true;
    }
}
