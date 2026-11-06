package com.eldercare.backend.care.internal.trip.domain;

import java.time.Instant;

/** Thông tin chuyến trong thân ActivitySessionCreate; lưu trên dòng activity_session. */
public record TripPlan(
        String destination,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        String transport,
        boolean facilityVehicle) {}
