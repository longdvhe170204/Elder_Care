package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.care.internal.trip.domain.Trip;
import com.eldercare.backend.care.internal.trip.domain.TripParticipant;
import com.eldercare.backend.care.internal.trip.domain.TripPlan;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.care.internal.trip.service.TripPreparationService;
import com.eldercare.backend.care.internal.trip.service.TripReadSources;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 FR-034, 014 module-interfaces (`ChuyenDiBuaAnPort`; chủ dự án duyệt 1a 2026-11-11): chuyến Đã lên lịch hoặc Đang đi
 * có giờ rời không sau giờ bữa và giờ về sau giờ bữa; người tham gia Được đi hoặc Đã rời viện; giờ về dự kiến hiện hành.
 */
class FR_034_TripMealReadTest {

    private static final Instant MEAL = Instant.parse("2026-11-12T05:00:00Z");

    private static Trip trip(long id, Instant from, Instant to, String status) {
        return new Trip(id, 1, 3L, new TripPlan("Công viên", from, to, "Xe viện", true), status, 0);
    }

    @Test
    void FR_034_overlappingTrips_eligibleAndDepartedParticipants() {
        var store = mock(ActivityTripStore.class);
        when(store.active())
                .thenReturn(List.of(
                        trip(41, MEAL.minusSeconds(7200), MEAL.plusSeconds(3600), "SCHEDULED"),
                        trip(42, MEAL.plusSeconds(600), MEAL.plusSeconds(7200), "SCHEDULED"),
                        trip(43, MEAL.minusSeconds(7200), MEAL, "IN_PROGRESS")));
        when(store.participants(41))
                .thenReturn(List.of(
                        new TripParticipant(1, 5, "ELIGIBLE", null),
                        new TripParticipant(2, 6, "NOT_GOING", null),
                        new TripParticipant(3, 7, "DEPARTED", null)));
        var sources = new TripReadSources(store, mock(TripPreparationService.class));

        assertThat(sources.overlappingMealTime(MEAL)).singleElement().satisfies(v -> {
            assertThat(v.tripId()).isEqualTo(41);
            assertThat(v.residentIds()).containsExactly(5L, 7L);
            assertThat(v.plannedDepartureAt()).isEqualTo(MEAL.minusSeconds(7200));
            assertThat(v.expectedReturnAt()).isEqualTo(MEAL.plusSeconds(3600));
        });
    }
}
