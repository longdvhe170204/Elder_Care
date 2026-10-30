package com.cambae.backend.m07.internal.service;

import java.util.Collections;
import java.util.List;

/**
 * Context for schedule calculation containing absence intervals and floor scope.
 */
public record ScheduleContext(Long residentId, List<ResidentAbsenceInterval> absences, Long floorAreaId) {

    public ScheduleContext {
        if (absences == null) {
            absences = Collections.emptyList();
        }
    }

    public static ScheduleContext empty(Long residentId) {
        return new ScheduleContext(residentId, Collections.emptyList(), null);
    }
}
