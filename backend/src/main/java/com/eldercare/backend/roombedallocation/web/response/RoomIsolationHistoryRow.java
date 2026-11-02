package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.internal.isolation.domain.RoomIsolationFact;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** Chỉ các trường lịch sử đã duyệt trong api003. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomIsolationHistoryRow(
        RoomIsolationFact.Action action, Instant occurredAt, String actor, String reason, Long incidentId) {}
