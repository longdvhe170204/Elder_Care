package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleScheduleInput;
import java.time.Instant;

/** 019 T022: thân {@code VehicleScheduleCommand} của api-019; trường dùng theo lệnh. */
public record VehicleScheduleCommand(
        String reason, Instant plannedDepartureAt, Instant plannedReturnAt, Instant occurredAt) {

    public VehicleScheduleInput.Command input() {
        return new VehicleScheduleInput.Command(reason, plannedDepartureAt, plannedReturnAt, occurredAt);
    }
}
