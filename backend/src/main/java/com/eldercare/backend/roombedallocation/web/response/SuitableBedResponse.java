package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import java.time.Instant;
import java.util.List;

/** Kết quả chưa chọn giường; earliestStartAt là hạn sẵn sàng của giường đang dọn. */
public record SuitableBedResponse(
        LocationResponse location,
        Instant futureAllocationFrom,
        Instant earliestStartAt,
        List<ResidentSummarySchema> roommates,
        BedStatus state) {}
