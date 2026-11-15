package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.care.api.ResidentTripReadApi;
import com.eldercare.backend.incident.api.dto.SosTripView;
import com.eldercare.backend.incident.api.spi.SosTripPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Cài {@link SosTripPort} (FR-047e, 014 FR-068a) bằng {@code care.api.ResidentTripReadApi} (007 T063; 2026-11-11, A thay
 * C): {@code incident} đã phụ thuộc {@code care}, nên M04 không cài SPI của M05 (vòng module). Trưởng đoàn nhận qua nhóm
 * {@code TripLeader(tripId)} của 009; ở đây chỉ trả người đi cùng.
 */
@Component
public class TripSosSource implements SosTripPort {

    private final ResidentTripReadApi trips;

    public TripSosSource(ResidentTripReadApi trips) {
        this.trips = trips;
    }

    @Override
    public Optional<SosTripView> ongoing(long residentId) {
        return trips.ongoing(residentId).map(t -> new SosTripView(t.tripId(), t.companionIds()));
    }
}
