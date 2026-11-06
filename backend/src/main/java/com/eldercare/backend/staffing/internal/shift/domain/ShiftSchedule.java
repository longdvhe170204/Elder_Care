package com.eldercare.backend.staffing.internal.shift.domain;

import java.time.YearMonth;

public record ShiftSchedule(
        long id, YearMonth month, String scopeType, Long scopeId, String status, Long createdBy, long version) {}
