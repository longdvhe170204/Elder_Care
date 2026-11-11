package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.TripMealOverlapView;
import java.time.Instant;
import java.util.List;

/**
 * 014 cung cấp cho adapter {@code TripMealPort} của 011 (chủ dự án duyệt 2026-11-11): nutrition đã phụ thuộc care nên
 * care không cài SPI của nutrition. Chỉ đọc.
 */
public interface TripMealReadApi {

    /** Chuyến Đã lên lịch hoặc Đang đi có giờ rời không sau {@code mealTime} và giờ về sau {@code mealTime}. */
    List<TripMealOverlapView> overlappingMealTime(Instant mealTime);
}
