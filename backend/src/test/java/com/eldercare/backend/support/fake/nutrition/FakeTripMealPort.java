package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.nutrition.api.spi.TripMealPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Bản giả {@link TripMealPort} (014 cài): trả chuyến đã đặt có giờ bữa nằm trong [từ, giờ về dự kiến). */
public class FakeTripMealPort implements TripMealPort {

    private final List<TripOverlapView> trips = new ArrayList<>();

    public FakeTripMealPort trip(TripOverlapView trip) {
        trips.add(trip);
        return this;
    }

    @Override
    public List<TripOverlapView> overlappingMealTime(Instant mealTime) {
        return trips.stream()
                .filter(t -> !mealTime.isBefore(t.from()) && mealTime.isBefore(t.expectedReturn()))
                .toList();
    }
}
