package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleScheduleInput;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 019 T022: thân {@code VehicleScheduleWrite} của api-019 (glossary 25.3). */
public record VehicleScheduleWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long vehicleId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedDepartureAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedReturnAt,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"TRIP", "MEDICAL_VISIT", "NON_EMERGENCY_TRANSFER", "OTHER"})
        String purpose,

        Long tripId,
        Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long driverStaffId,
        String note) {

    public VehicleScheduleInput.Booking input() {
        return new VehicleScheduleInput.Booking(
                vehicleId, plannedDepartureAt, plannedReturnAt, purpose, tripId, residentId, driverStaffId, note);
    }
}
