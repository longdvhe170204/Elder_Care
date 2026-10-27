package com.cambae.backend.m04.api.dto;

import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record CareResultView(
        long id,
        Optional<Long> taskId,
        long residentId,
        ResultType resultType,
        Instant performedAt,
        Optional<Instant> deviceAt,
        long recordedById,
        Map<String, Object> values,
        Optional<String> note) {}
