package com.eldercare.backend.nutrition.api.events;

import java.time.Instant;
import java.util.Set;

/** 014 phát: lên lịch, đổi người tham gia, gia hạn giờ về, hủy chuyến, đính chính điểm danh. */
public record TripMealChanged(
        long tripId, Set<Long> residentIds, Instant from, Instant expectedUntil, String changeType, String eventKey) {}
