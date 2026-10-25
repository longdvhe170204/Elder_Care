package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.dto.BedStatus;
import java.time.Instant;
import java.util.List;

/** Kết quả chưa chọn giường; earliestStartAt là hạn sẵn sàng của giường đang dọn. */
public record SuitableBedResponse(
        LocationResponse location,
        Instant futureAllocationFrom,
        Instant earliestStartAt,
        List<ResidentSummarySchema> roommates,
        BedStatus state) {}
