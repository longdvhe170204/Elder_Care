package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
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
