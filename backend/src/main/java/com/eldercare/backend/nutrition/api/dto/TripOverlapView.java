package com.eldercare.backend.nutrition.api.dto;

import java.time.Instant;
import java.util.Set;

/** Chuyến đi đã lên lịch (014) trùng giờ bữa: người tham gia và giờ trở lại dự kiến. */
public record TripOverlapView(long tripId, Set<Long> residentIds, Instant from, Instant expectedReturn) {}
