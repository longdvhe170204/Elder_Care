package com.eldercare.backend.roombedallocation.web.response;

import java.time.Instant;

/** Dòng lịch sử theo người theo đúng hợp đồng HTTP T048. */
public record ResidentLocationHistoryRow(
        long allocationId,
        LocationResponse location,
        Instant from,
        Instant to,
        boolean corrected,
        boolean pendingBackfill) {}
