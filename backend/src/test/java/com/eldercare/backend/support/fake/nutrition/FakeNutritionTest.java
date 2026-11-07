package com.eldercare.backend.support.fake.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.nutrition.api.events.StayMealChanged;
import com.eldercare.backend.nutrition.api.events.TripMealChanged;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 011 T002: bản giả của hai port 012, 014 trả dữ liệu đặt trước và phát sự kiện đổi. */
class FakeNutritionTest {

    private static final Instant T0 = Instant.parse("2026-11-10T04:00:00Z");

    @Test
    void fakeStayMealPort_returnsOnlyStaysCoveringInstant() {
        FakeStayMealPort fake = new FakeStayMealPort();
        fake.stay(new StayMealView(1, 100, 200, T0.minusSeconds(3600), T0.plusSeconds(3600), true))
                .stay(new StayMealView(2, 101, 201, T0.plusSeconds(7200), T0.plusSeconds(9000), true))
                .stay(new StayMealView(3, 102, 202, T0.minusSeconds(3600), T0.plusSeconds(3600), false));

        assertThat(fake.staysWithMealRegistration(T0))
                .extracting(StayMealView::stayId)
                .containsExactly(1L);
    }

    @Test
    void fakeTripMealPort_returnsTripsOverlappingMealTime() {
        FakeTripMealPort fake = new FakeTripMealPort();
        fake.trip(new TripOverlapView(7, Set.of(100L, 101L), T0.minusSeconds(60), T0.plusSeconds(600)));

        assertThat(fake.overlappingMealTime(T0))
                .extracting(TripOverlapView::tripId)
                .containsExactly(7L);
        assertThat(fake.overlappingMealTime(T0.plusSeconds(3600))).isEmpty();
    }

    @Test
    void fakeEvents_publishStayAndTripChanges() {
        List<Object> published = new ArrayList<>();
        FakeNutritionEvents events = new FakeNutritionEvents(published::add);

        StayMealChanged stay = events.stayChanged(1, 100, 200, "STARTED", true, T0, T0.plusSeconds(60));
        TripMealChanged trip = events.tripChanged(7, Set.of(100L), T0, T0.plusSeconds(60), "SCHEDULED");

        assertThat(published).containsExactly(stay, trip);
        assertThat(stay.eventKey()).isNotEqualTo(trip.eventKey());
    }
}
