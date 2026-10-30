package com.eldercare.backend.roombedallocation.web.response;

import java.time.Instant;

/** Vị trí và phân bổ chỉ trả sau khi service kiểm phạm vi. */
public record ResidentLocationResponse(
        LocationResponse location,
        BedAllocationResponse currentAllocation,
        BedAllocationResponse futureAllocation,
        Instant withoutBedSince) {}
