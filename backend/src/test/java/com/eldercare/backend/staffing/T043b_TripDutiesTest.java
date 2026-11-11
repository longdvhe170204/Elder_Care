package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.TripDutyReadApi;
import com.eldercare.backend.care.api.dto.TripInShiftView;
import com.eldercare.backend.staffing.internal.duty.service.TripDuties;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 008 T043b (FR-039 (e), 002 FR-019): adapter {@code TripDutyPort} của M09 đọc {@code care.api.TripDutyReadApi} (hướng
 * chủ dự án duyệt 05/11/2026), giữ nguyên trưởng đoàn, người đi cùng, chuyến trong ca.
 */
class T043b_TripDutiesTest {

    @Test
    void T043b_delegatesToCareTripDuties() {
        Instant departed = Instant.parse("2026-11-11T02:00:00Z");
        Instant back = Instant.parse("2026-11-11T06:00:00Z");
        var duties = new TripDuties(new TripDutyReadApi() {
            @Override
            public Optional<Long> groupLeader(long tripId) {
                return tripId == 41 ? Optional.of(7L) : Optional.empty();
            }

            @Override
            public List<Long> companions(long tripId) {
                return List.of(8L);
            }

            @Override
            public List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to) {
                return List.of(new TripInShiftView(41, List.of(7L, 8L), back, List.of(5L), departed));
            }
        });

        assertThat(duties.groupLeader(41)).contains(7L);
        assertThat(duties.groupLeader(42)).isEmpty();
        assertThat(duties.companions(41)).containsExactly(8L);
        assertThat(duties.inShift(3, departed, back)).singleElement().satisfies(t -> {
            assertThat(t.tripId()).isEqualTo(41);
            assertThat(t.staffIds()).containsExactly(7L, 8L);
            assertThat(t.residentIds()).containsExactly(5L);
            assertThat(t.expectedReturnAt()).isEqualTo(back);
            assertThat(t.departedAt()).isEqualTo(departed);
        });
    }
}
