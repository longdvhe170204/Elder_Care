package com.eldercare.backend.care.api.dto;

import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record AttendanceView(
        long id,
        long residentId,
        LocalDate date,
        AttendanceStatus status,
        Optional<LocalTime> expectedArrival,
        Optional<LocalTime> expectedReturn,
        Optional<LocalTime> currentExpectedReturn,
        Optional<Instant> arrivedAt,
        Optional<Instant> leftAt,
        boolean adHocSession) {}
