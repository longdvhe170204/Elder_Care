package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.dto.BedStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** Hợp đồng lịch sử trạng thái Phase 6. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedStateHistoryRow(
        long id,
        BedStatus from,
        BedStatus to,
        String event,
        Instant occurredAt,
        Instant plannedAt,
        boolean executedLate,
        String actor,
        String reason,
        HistoryBasis basis,
        Boolean disinfected,
        Boolean ppeUsed,
        String note,
        boolean corrected) {
    public record HistoryBasis(String type, long id) {}
}
