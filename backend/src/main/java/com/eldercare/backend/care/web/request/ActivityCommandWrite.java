package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.trip.domain.TripTemplatePlan;
import java.time.LocalDate;
import java.util.List;

/** 014 T004: thân {@code ActivityCommand} của api-014 (glossary 20.3). */
public record ActivityCommandWrite(
        String reason,
        LocalDate effectiveDate,
        ActivityTemplate template,
        LocalDate fromDate,
        LocalDate toDate,
        Long residentId,
        TripTemplatePlan trip) {
    public record ActivityTemplate(
            List<Integer> daysOfWeek,
            Integer intervalDays,
            String startTime,
            String endTime,
            LocalDate startDate,
            LocalDate endDate) {}
}
