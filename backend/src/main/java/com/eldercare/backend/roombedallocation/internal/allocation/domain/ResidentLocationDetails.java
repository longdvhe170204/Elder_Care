package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;

/** Metadata phân bổ để trống khi người xem chỉ có phạm vi xem vị trí hiện tại. */
public record ResidentLocationDetails(
        LocationView location,
        AllocationDetails currentAllocation,
        AllocationDetails futureAllocation,
        Instant withoutBedSince) {}
