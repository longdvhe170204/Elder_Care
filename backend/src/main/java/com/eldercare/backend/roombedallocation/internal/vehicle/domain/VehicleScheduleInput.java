package com.eldercare.backend.roombedallocation.internal.vehicle.domain;

import java.time.Instant;

/** Thân đặt lịch (api-019 {@code VehicleScheduleWrite}) và thân lệnh ({@code VehicleScheduleCommand}). */
public final class VehicleScheduleInput {
    private VehicleScheduleInput() {}

    public record Booking(
            Long vehicleId,
            Instant plannedDepartureAt,
            Instant plannedReturnAt,
            String purpose,
            Long tripId,
            Long residentId,
            Long driverStaffId,
            String note) {}

    public record Command(String reason, Instant plannedDepartureAt, Instant plannedReturnAt, Instant occurredAt) {}
}
