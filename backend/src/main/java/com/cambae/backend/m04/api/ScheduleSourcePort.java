package com.cambae.backend.m04.api;

import java.time.LocalDate;
import java.util.List;

/**
 * SPI port for Module M04 (005) daily resident timetable integration (FR-048).
 */
public interface ScheduleSourcePort {

    /**
     * Resolves periodic medication doses for a resident on the specified date.
     */
    List<MedicationChecklistItem> byDate(long residentId, LocalDate date);
}
