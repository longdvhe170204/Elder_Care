package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.dto.TripMealOverlapView;
import com.eldercare.backend.nutrition.internal.closing.service.TripMeals;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T042 (FR-034): adapter {@code TripMealPort} của M08 đọc {@code care.api.TripMealReadApi} (chủ dự án duyệt 1a
 * 2026-11-11; care không cài được SPI của nutrition vì nutrition đã phụ thuộc care).
 */
class T042_TripMealsTest {

    @Test
    void T042_mapsCareTripsToMealOverlaps() {
        Instant meal = Instant.parse("2026-11-12T05:00:00Z");
        var port = new TripMeals(at -> at.equals(meal)
                ? List.of(new TripMealOverlapView(41, List.of(5L, 7L), meal.minusSeconds(7200), meal.plusSeconds(3600)))
                : List.of());

        assertThat(port.overlappingMealTime(meal)).singleElement().satisfies(t -> {
            assertThat(t.tripId()).isEqualTo(41);
            assertThat(t.residentIds()).containsExactlyInAnyOrder(5L, 7L);
            assertThat(t.from()).isEqualTo(meal.minusSeconds(7200));
            assertThat(t.expectedReturn()).isEqualTo(meal.plusSeconds(3600));
        });
        assertThat(port.overlappingMealTime(meal.plusSeconds(1))).isEmpty();
    }
}
