package com.eldercare.backend.care.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * SPI port for Module M04 (005) shift checklist integration (FR-029).
 */
public interface MedicationDoseChecklistPort {

    /**
     * Resolves medication doses for the specified residents within a shift interval.
     */
    List<MedicationChecklistItem> byShift(Collection<Long> residentIds, Instant from, Instant to);
}
