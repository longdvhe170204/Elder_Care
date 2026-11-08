package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.roombedallocation.api.dto.VehicleSchedulePurpose;
import com.eldercare.backend.roombedallocation.api.dto.VehicleScheduleStatus;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 019 T022: phản hồi {@code VehicleSchedule} của api-019 (bổ sung 2026-11-06, chủ dự án duyệt). */
@Schema(name = "VehicleSchedule")
public record VehicleScheduleResponse(
        Long id,
        Long vehicleId,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        VehicleSchedulePurpose purpose,
        Long tripId,
        Long residentId,
        Long driverStaffId,
        Long bookedByStaffId,
        VehicleScheduleStatus status,
        Instant departedAt,
        Instant returnedAt,
        String cancellationReason) {

    public static VehicleScheduleResponse of(SimpleRecord<VehicleSchedule> row) {
        var c = row.content();
        return new VehicleScheduleResponse(
                row.id(),
                c.vehicleId(),
                c.plannedDepartureAt(),
                c.plannedReturnAt(),
                VehicleSchedulePurpose.valueOf(c.purpose()),
                c.tripId(),
                c.residentId(),
                c.driverStaffId(),
                c.bookedByStaffId(),
                VehicleScheduleStatus.valueOf(row.status()),
                c.departedAt(),
                c.returnedAt(),
                c.cancellationReason());
    }
}
