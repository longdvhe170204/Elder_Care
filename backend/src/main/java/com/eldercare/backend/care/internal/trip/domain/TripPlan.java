package com.eldercare.backend.care.internal.trip.domain;

import com.eldercare.backend.common.error.InvalidValueException;
import java.time.Instant;

/** Thông tin chuyến trong thân ActivitySessionCreate; lưu trên dòng activity_session. */
public record TripPlan(
        String destination,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        String transport,
        boolean facilityVehicle) {
    public void validate() {
        if (destination == null
                || destination.isBlank()
                || transport == null
                || transport.isBlank()
                || plannedDepartureAt == null
                || plannedReturnAt == null
                || !plannedReturnAt.isAfter(plannedDepartureAt))
            throw new InvalidValueException("Chuyến cần điểm đến, phương tiện và giờ về sau giờ rời");
    }
}
