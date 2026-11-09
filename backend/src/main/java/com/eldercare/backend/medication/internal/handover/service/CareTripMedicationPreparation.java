package com.eldercare.backend.medication.internal.handover.service;

import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.medication.api.TripMedicationApi;
import com.eldercare.backend.medication.api.dto.TimeInterval;
import com.eldercare.backend.medication.api.dto.TripChangeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@code care.api.spi.TripMedicationPreparationPort} của 014 bằng {@link TripMedicationApi} (hướng API Phase 6 của
 * 014, chủ dự án duyệt 2026-11-05): care khai, medication cài, không tạo phụ thuộc care → medication.
 */
@Component
public class CareTripMedicationPreparation implements TripMedicationPreparationPort {

    private final TripMedicationApi trips;

    public CareTripMedicationPreparation(TripMedicationApi trips) {
        this.trips = trips;
    }

    @Override
    public boolean isPrepared(long residentId, long tripId) {
        return trips.isPrepared(residentId, tripId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTripChanged(long tripId, long residentId, TripMedicationChangeType changeType) {
        trips.onTripChanged(tripId, residentId, TripChangeType.valueOf(changeType.name()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculateInterval(long tripId, long residentId, TripInterval newInterval) {
        trips.recalculateInterval(tripId, residentId, new TimeInterval(newInterval.from(), newInterval.to()));
    }
}
