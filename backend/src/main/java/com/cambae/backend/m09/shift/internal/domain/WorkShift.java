package com.cambae.backend.m09.shift.internal.domain;

import java.time.Instant;

public record WorkShift(
        long id,
        long scheduleId,
        long shiftTemplateId,
        Instant startsAt,
        Instant endsAt,
        String scopeType,
        Long scopeId,
        boolean handoverRequired,
        String status,
        Long leadStaffId,
        boolean missingLead,
        long version) {}
