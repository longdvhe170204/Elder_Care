package com.eldercare.backend.roombedallocation.internal.asset.domain;

import java.time.LocalDate;

/** Thân ghi tăng, sửa mô tả (api-019 {@code AssetWrite}). */
public record AssetInput(
        String code,
        Long typeId,
        String name,
        Long departmentId,
        String source,
        String sourceDescription,
        Long procurementRequestId,
        Long acquisitionCost,
        LocalDate inServiceOn,
        Integer maintenanceIntervalDays,
        LocalDate maintenanceDueOn,
        String licensePlate,
        Integer seatCount,
        LocalDate vehicleInspectionDueOn,
        LocalDate insuranceDueOn) {}
