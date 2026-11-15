package com.eldercare.backend.administration.parameter.internal.domain;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Một lần đổi giá trị: dòng {@code status_history} loại {@code M15.PARAMETER} (DBR-24; FR-019). */
public record ParameterHistoryRow(JsonNode value, Instant from, Long actorId, String reason, boolean defaultRestore) {}
