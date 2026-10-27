package com.cambae.backend.m04.api.dto;

import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record TaskReportView(
        long id,
        Optional<Long> residentId,
        long taskTypeId,
        long floorAreaId,
        long shiftId,
        TaskStatus status,
        List<TaskStateChange> history,
        Importance importance,
        Optional<Long> performerId,
        Optional<Long> closedById,
        Optional<String> closingRule,
        Instant plannedAt,
        Optional<Instant> performedAt,
        Optional<Instant> deviceAt,
        boolean recordedLate,
        String syncLabel,
        List<Instant> overdueAt) {}
