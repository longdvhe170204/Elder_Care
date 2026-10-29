package com.eldercare.backend.medication.api.spi;

import java.time.Instant;

/**
 * Shift information returned from module M09.
 */
public record ShiftView(long shiftId, long floorAreaId, Instant startTime, Instant endTime, Long shiftLeadId) {}
