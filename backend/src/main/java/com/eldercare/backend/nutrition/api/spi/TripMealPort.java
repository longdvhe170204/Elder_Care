package com.eldercare.backend.nutrition.api.spi;

import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import java.time.Instant;
import java.util.List;

/** 014 cài; chỉ đọc. Thiếu bean thì 011 không trừ người đi chuyến đã lên lịch. */
public interface TripMealPort {
    /** Chuyến đã lên lịch trùng giờ bữa {@code mealTime}. */
    List<TripOverlapView> overlappingMealTime(Instant mealTime);
}
