package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 014 T009: thân {@code ActivitySessionCreate} của api-014 (glossary 20.3); phần chuyến đi cài ở US4. */
public record ActivitySessionCreateWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long activityId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endsAt,
        Long locationId,
        Long responsibleStaffId,
        ActivitySessionTrip trip) {
    public record ActivitySessionTrip(
            String destination,
            Instant plannedDepartureAt,
            Instant plannedReturnAt,
            String transport,
            Boolean facilityVehicle) {}
}
