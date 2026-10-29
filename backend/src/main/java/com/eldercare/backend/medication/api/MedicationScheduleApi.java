package com.eldercare.backend.medication.api;

import com.eldercare.backend.medication.api.dto.DoseDisplayView;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * API for reading medication schedules computed from prescriptions.
 * Used by Module M04 (005 via checklist SPI), M09 (008), M14 (016).
 */
public interface MedicationScheduleApi {
    /** Medication administration schedule of a resident within a time range. */
    List<DoseDisplayView> byResident(long residentId, Instant from, Instant to);

    /** List of staff IDs who recorded doses or PRN administrations in interval (007 FR-060c). */
    Set<Long> staffRecordedDose(long residentId, Instant from, Instant to);
}
