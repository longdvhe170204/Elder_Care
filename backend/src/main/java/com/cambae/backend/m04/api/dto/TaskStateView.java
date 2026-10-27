package com.cambae.backend.m04.api.dto;

import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record TaskStateView(
        long id,
        TaskStatus status,
        Optional<Long> closedById,
        Optional<Instant> closedAt,
        Optional<String> closingRule) {}
