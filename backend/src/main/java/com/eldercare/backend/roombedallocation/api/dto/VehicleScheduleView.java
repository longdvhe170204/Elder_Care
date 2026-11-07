package com.eldercare.backend.roombedallocation.api.dto;

import java.time.Instant;

/** 019 contracts mục 2.3 (LichXeView); departedAt, returnedAt rỗng khi chưa ghi. */
public record VehicleScheduleView(
        long scheduleId,
        long vehicleId,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        VehicleSchedulePurpose purpose,
        Long tripId,
        VehicleScheduleStatus status,
        Instant departedAt,
        Instant returnedAt) {}
