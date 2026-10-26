package com.cambae.backend.support.fake.m09;

import com.cambae.backend.m09.api.spi.TripDutyPort;
import com.cambae.backend.m09.api.spi.TripInShiftView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Nguồn chuyến đi giả; test đặt người có nhiệm vụ và khoảng đi thực tế, không kiểm luật của 014. */
public class FakeTripDuties implements TripDutyPort {

    private final Map<Long, Long> leaders = new LinkedHashMap<>();
    private final Map<Long, List<Long>> companions = new LinkedHashMap<>();
    private final Map<Long, TripEntry> trips = new LinkedHashMap<>();
    private boolean available = true;

    public FakeTripDuties groupLeader(long tripId, long staffId) {
        leaders.put(tripId, staffId);
        return this;
    }

    public FakeTripDuties companions(long tripId, List<Long> staffIds) {
        companions.put(tripId, List.copyOf(staffIds));
        return this;
    }

    /** returnedAt rỗng nghĩa là chuyến chưa về; giờ về dự kiến không cắt khoảng thực tế. */
    public FakeTripDuties trip(long floorAreaId, Instant departedAt, Instant returnedAt, TripInShiftView view) {
        trips.put(view.tripId(), new TripEntry(floorAreaId, departedAt, returnedAt, view));
        return this;
    }

    public FakeTripDuties available(boolean value) {
        available = value;
        return this;
    }

    @Override
    public Optional<Long> groupLeader(long tripId) {
        requireAvailable();
        return Optional.ofNullable(leaders.get(tripId));
    }

    @Override
    public List<Long> companions(long tripId) {
        requireAvailable();
        return companions.getOrDefault(tripId, List.of());
    }

    @Override
    public List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to) {
        requireAvailable();
        return trips.values().stream()
                .filter(trip -> trip.floorAreaId() == floorAreaId)
                .filter(trip -> trip.departedAt().isBefore(to))
                .filter(trip -> trip.returnedAt() == null || trip.returnedAt().isAfter(from))
                .map(TripEntry::view)
                .toList();
    }

    private void requireAvailable() {
        if (!available) {
            throw new IllegalStateException("Nguồn chuyến đi giả chưa có dữ liệu");
        }
    }

    private record TripEntry(long floorAreaId, Instant departedAt, Instant returnedAt, TripInShiftView view) {}
}
