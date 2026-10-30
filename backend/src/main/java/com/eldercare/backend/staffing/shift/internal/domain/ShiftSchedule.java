package com.eldercare.backend.staffing.shift.internal.domain;

import java.time.YearMonth;

public record ShiftSchedule(
        long id, YearMonth month, String scopeType, Long scopeId, String status, Long createdBy, long version) {}
