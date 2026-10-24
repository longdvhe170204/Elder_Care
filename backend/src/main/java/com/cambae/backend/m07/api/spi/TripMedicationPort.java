package com.cambae.backend.m07.api.spi;

import com.cambae.backend.m07.api.dto.TimeInterval;
import java.util.Optional;

/**
 * SPI declared by M07 and implemented by Module M04 (014) to provide trip / hospital transfer information.
 * Read-only.
 */
public interface TripMedicationPort {
    /** Expected trip interval when resident is evaluated as Eligible. */
    Optional<TimeInterval> approvedTripInterval(long residentId, long tripId);

    /** Trip duration interval. */
    Optional<TimeInterval> tripInterval(long tripId);
}
