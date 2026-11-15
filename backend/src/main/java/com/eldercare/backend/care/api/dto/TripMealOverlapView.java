package com.eldercare.backend.care.api.dto;

import java.time.Instant;
import java.util.List;

/** Chuyến trùng giờ bữa cho 011 FR-034: người tham gia Được đi hoặc Đã rời viện, giờ rời và giờ về dự kiến hiện hành. */
public record TripMealOverlapView(
        long tripId, List<Long> residentIds, Instant plannedDepartureAt, Instant expectedReturnAt) {
    public TripMealOverlapView {
        residentIds = List.copyOf(residentIds);
    }
}
