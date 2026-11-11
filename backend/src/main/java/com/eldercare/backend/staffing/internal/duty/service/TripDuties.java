package com.eldercare.backend.staffing.internal.duty.service;

import com.eldercare.backend.care.api.TripDutyReadApi;
import com.eldercare.backend.staffing.api.spi.TripDutyPort;
import com.eldercare.backend.staffing.api.spi.TripInShiftView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter {@link TripDutyPort} sang {@code care.api.TripDutyReadApi} (hướng chủ dự án duyệt 05/11/2026; 008 T043b,
 * 2026-11-11, A thay E): trưởng đoàn, người đi cùng cho 002 FR-019 và chuyến trong ca cho nguồn bàn giao FR-039 (e).
 */
@Component
public class TripDuties implements TripDutyPort {

    private final TripDutyReadApi trips;

    public TripDuties(TripDutyReadApi trips) {
        this.trips = trips;
    }

    @Override
    public Optional<Long> groupLeader(long tripId) {
        return trips.groupLeader(tripId);
    }

    @Override
    public List<Long> companions(long tripId) {
        return trips.companions(tripId);
    }

    @Override
    public List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to) {
        return trips.inShift(floorAreaId, from, to).stream()
                .map(t -> new TripInShiftView(
                        t.tripId(), t.staffIds(), t.expectedReturnAt(), t.residentIds(), t.departedAt()))
                .toList();
    }
}
