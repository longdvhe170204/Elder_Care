package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.care.api.TripMealReadApi;
import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.nutrition.api.spi.TripMealPort;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Cài {@link TripMealPort} (011 FR-034; T042, 2026-11-11) bằng {@code care.api.TripMealReadApi}: nutrition đã phụ thuộc
 * care, nên M04 không cài SPI của M08 (vòng module).
 */
@Component
public class TripMeals implements TripMealPort {

    private final TripMealReadApi trips;

    public TripMeals(TripMealReadApi trips) {
        this.trips = trips;
    }

    @Override
    public List<TripOverlapView> overlappingMealTime(Instant mealTime) {
        return trips.overlappingMealTime(mealTime).stream()
                .map(t -> new TripOverlapView(
                        t.tripId(), Set.copyOf(t.residentIds()), t.plannedDepartureAt(), t.expectedReturnAt()))
                .toList();
    }
}
