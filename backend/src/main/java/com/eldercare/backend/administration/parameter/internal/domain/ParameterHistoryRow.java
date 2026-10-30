package com.eldercare.backend.administration.parameter.internal.domain;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Một lần đổi giá trị ở {@code parameter_history} (DBR-24; FR-019). */
public record ParameterHistoryRow(JsonNode value, Instant from, Long actorId, String reason, boolean defaultRestore) {}
