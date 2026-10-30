package com.eldercare.backend.administration.parameter.internal.domain;

import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Một dòng {@code parameter} (R-07; FR-011 → FR-022). */
public record ParameterRow(
        String code,
        String description,
        ParameterKind kind,
        String unit,
        JsonNode constraints,
        JsonNode defaultValue,
        JsonNode currentValue,
        String usedIn,
        int version,
        Instant changedAt) {}
