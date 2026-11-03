package com.eldercare.backend.care.api.dto;

import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record CarePlanView(
        long id,
        long residentId,
        int revision,
        CarePlanStatus status,
        LocalDate effectiveFrom,
        Optional<LocalDate> effectiveTo,
        List<CarePlanItemView> items) {}
