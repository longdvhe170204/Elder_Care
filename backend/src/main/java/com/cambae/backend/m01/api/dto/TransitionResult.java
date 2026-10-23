package com.cambae.backend.m01.api.dto;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Kết quả một lần chuyển trạng thái, đưa cho {@code applyEffects} và trả cho người gọi. */
public record TransitionResult(
        long residentId,
        ResidentStatus from,
        ResidentStatus to,
        StatusCommand command,
        long historyId,
        Instant at,
        JsonNode details) {}
