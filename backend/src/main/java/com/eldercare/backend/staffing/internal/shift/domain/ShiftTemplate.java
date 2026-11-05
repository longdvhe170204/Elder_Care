package com.eldercare.backend.staffing.internal.shift.domain;

import java.time.LocalTime;

public record ShiftTemplate(
        long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        String scopeType,
        boolean handoverRequired,
        boolean active,
        long version) {}
