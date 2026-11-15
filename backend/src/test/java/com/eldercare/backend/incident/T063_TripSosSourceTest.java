package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.dto.ResidentOngoingTripView;
import com.eldercare.backend.incident.internal.alert.service.TripSosSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 007 T063 (FR-047e, 014 FR-068a): bản cài {@code SosTripPort} của M05 đọc chuyến đang diễn ra qua
 * {@code care.api.ResidentTripReadApi}; người đi cùng lấy từ chuyến, trưởng đoàn nhận qua nhóm {@code TripLeader}.
 */
class T063_TripSosSourceTest {

    @Test
    void T063_ongoingTrip_mapsCompanions_noTripIsEmpty() {
        var source = new TripSosSource(resident -> resident == 5
                ? Optional.of(new ResidentOngoingTripView(41, Optional.of(7L), List.of(8L, 9L)))
                : Optional.empty());

        var trip = source.ongoing(5).orElseThrow();
        assertThat(trip.tripId()).isEqualTo(41);
        assertThat(trip.companionStaffIds()).containsExactly(8L, 9L);
        assertThat(source.ongoing(6)).isEmpty();
    }
}
