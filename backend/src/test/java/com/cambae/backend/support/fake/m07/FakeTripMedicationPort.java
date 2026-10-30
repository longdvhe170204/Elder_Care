package com.cambae.backend.support.fake.m07;

import com.cambae.backend.m07.api.dto.TimeInterval;
import com.cambae.backend.m07.api.spi.TripMedicationPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bản giả {@link TripMedicationPort} (Module 014 cài): trả về khoảng thời gian chuyến đi của người cao tuổi.
 */
public class FakeTripMedicationPort implements TripMedicationPort {

    private final Map<String, TimeInterval> approvedTrips = new HashMap<>();
    private final Map<Long, TimeInterval> trips = new HashMap<>();

    public FakeTripMedicationPort approvedTrip(long residentId, long tripId, TimeInterval interval) {
        approvedTrips.put(residentId + ":" + tripId, interval);
        return this;
    }

    public FakeTripMedicationPort trip(long tripId, TimeInterval interval) {
        trips.put(tripId, interval);
        return this;
    }

    @Override
    public Optional<TimeInterval> approvedTripInterval(long residentId, long tripId) {
        return Optional.ofNullable(approvedTrips.get(residentId + ":" + tripId));
    }

    @Override
    public Optional<TimeInterval> tripInterval(long tripId) {
        return Optional.ofNullable(trips.get(tripId));
    }
}
