package com.cambae.backend.m07.api.spi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SPI declared by M07 and implemented by Module M09 (008) to provide shift and nurse assignment information.
 * Read-only.
 */
public interface ShiftNursePort {
    /** Shift on the floor covering the specified time. */
    Optional<ShiftView> shiftContaining(long floorAreaId, Instant time);

    /** Nurse assigned to the resident during the shift. */
    Optional<Long> assignedNurse(long residentId, Instant time);

    /** List of nurses on shift on the floor at the specified time. */
    List<Long> nursesOnShift(long floorAreaId, Instant time);

    /** Check whether staff member is on an active shift. */
    boolean isStaffOnActiveShift(long staffId, Instant time);

    /** Check whether shift handover has been confirmed. */
    boolean isHandoverConfirmed(long shiftId);
}
