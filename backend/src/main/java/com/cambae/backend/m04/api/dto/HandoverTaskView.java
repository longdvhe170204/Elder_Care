package com.cambae.backend.m04.api.dto;

import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record HandoverTaskView(
        long id,
        Optional<Long> residentId,
        long taskTypeId,
        Instant plannedAt,
        TaskStatus status,
        Importance importance,
        Optional<Long> performerId,
        int reminderCount,
        List<Long> alertIds) {}
