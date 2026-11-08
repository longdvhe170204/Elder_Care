package com.eldercare.backend.medication.internal.handover.service;

import com.eldercare.backend.care.api.TripMedicationReadApi;
import com.eldercare.backend.medication.api.dto.TimeInterval;
import com.eldercare.backend.medication.api.spi.TripMedicationPort;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Cài {@link TripMedicationPort} của 006 bằng {@code care.api.TripMedicationReadApi} của 014 (hướng API Phase 6 của 014,
 * chủ dự án duyệt 2026-11-05). Chưa có nguồn 014 thì không có chuyến nào.
 */
@Component
public class CareTripMedicationPort implements TripMedicationPort {

    private final ObjectProvider<TripMedicationReadApi> trips;

    public CareTripMedicationPort(ObjectProvider<TripMedicationReadApi> trips) {
        this.trips = trips;
    }

    @Override
    public Optional<TimeInterval> approvedTripInterval(long residentId, long tripId) {
        TripMedicationReadApi api = trips.getIfAvailable();
        return api == null
                ? Optional.empty()
                : api.approvedTripInterval(residentId, tripId).map(i -> new TimeInterval(i.from(), i.to()));
    }

    @Override
    public Optional<TimeInterval> tripInterval(long tripId) {
        TripMedicationReadApi api = trips.getIfAvailable();
        return api == null ? Optional.empty() : api.tripInterval(tripId).map(i -> new TimeInterval(i.from(), i.to()));
    }
}
