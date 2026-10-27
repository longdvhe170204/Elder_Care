package com.cambae.backend.m04.api.dto;

import com.cambae.backend.common.Role;
import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record CarePlanItemView(
        long id,
        long taskTypeId,
        String content,
        String frequencyType,
        Optional<Integer> intervalHours,
        Optional<Integer> timesPerDay,
        Optional<LocalTime> startsAt,
        List<LocalTime> times,
        Optional<Integer> weekdays,
        int windowBeforeMinutes,
        int windowAfterMinutes,
        Role performerRole,
        ResultType resultType,
        Importance importance,
        boolean billable,
        Optional<Integer> waterTargetMl) {}
