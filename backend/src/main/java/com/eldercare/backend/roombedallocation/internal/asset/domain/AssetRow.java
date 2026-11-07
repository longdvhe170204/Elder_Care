package com.eldercare.backend.roombedallocation.internal.asset.domain;

import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/** Một dòng {@code asset} (019 data-model mục 4). */
public record AssetRow(
        long id,
        String code,
        long typeId,
        String name,
        long departmentId,
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
        LocalDate insuranceDueOn,
        Long bedId,
        AssetStatus status,
        String locationType,
        Long locationId,
        Long floorAreaId,
        boolean maintenanceOverdue,
        boolean vehicleInspectionOverdue,
        boolean insuranceOverdue,
        boolean awaitingResidentTransfer,
        int version) {

    public Set<AssetFlag> flags() {
        var flags = EnumSet.noneOf(AssetFlag.class);
        if (maintenanceOverdue) flags.add(AssetFlag.MAINTENANCE_OVERDUE);
        if (vehicleInspectionOverdue) flags.add(AssetFlag.VEHICLE_INSPECTION_OVERDUE);
        if (insuranceOverdue) flags.add(AssetFlag.INSURANCE_OVERDUE);
        if (awaitingResidentTransfer) flags.add(AssetFlag.AWAITING_RESIDENT_TRANSFER);
        return flags;
    }
}
