package com.cambae.backend.m07.api.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Currently active medication for emergency card and hospital transfer summary (FR-050).
 */
public record ActiveMedicationView(
        long prescriptionId,
        MedicationSummary medication,
        String dosage,
        String routeOfAdministration,
        List<LocalTime> scheduledTimes,
        String prnLimits,
        MedicationSource source,
        String status) {}
