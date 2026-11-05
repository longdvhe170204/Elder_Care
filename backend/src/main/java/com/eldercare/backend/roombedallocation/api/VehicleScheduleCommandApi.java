package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 019 contracts mục 2.2: thay đổi lịch xe trong cùng transaction chuyến của 014. */
public interface VehicleScheduleCommandApi {
    @Transactional(propagation = Propagation.MANDATORY)
    void followTrip(
            long tripId,
            TripVehicleChangeType type,
            Instant plannedDepartureAt,
            Instant plannedReturnAt,
            Optional<Instant> actualAt);
}
