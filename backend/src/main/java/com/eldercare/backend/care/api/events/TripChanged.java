package com.eldercare.backend.care.api.events;

import java.time.Instant;
import java.util.Optional;

/**
 * 014 contracts mục 1.2 (ChuyenDiThayDoi) cho 011 (ChuyenDiDaDoi, chủ dự án duyệt 2026-11-05 đặt ở care) và 016. type:
 * SCHEDULED, CHANGED, CANCELLED, FIRST_DEPARTURE, EXTENDED, LAST_DEPARTURE_UNDONE, RETURN_FINISHED.
 */
public record TripChanged(
        long tripId,
        String type,
        boolean facilityVehicle,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        Optional<Instant> actualAt,
        String eventKey) {}
