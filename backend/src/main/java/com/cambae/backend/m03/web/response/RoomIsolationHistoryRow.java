package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.internal.isolation.domain.RoomIsolationFact;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** Chỉ các trường lịch sử đã duyệt trong api003. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomIsolationHistoryRow(
        RoomIsolationFact.Action action, Instant occurredAt, String actor, String reason, Long incidentId) {}
