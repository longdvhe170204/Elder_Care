package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetInput;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 019 T018: thân {@code AssetWrite} của api-019 (glossary 25.3); ghi tăng và sửa mô tả. */
public record AssetWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long typeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long departmentId,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PURCHASED", "DONATED", "TRANSFERRED_IN"})
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
        LocalDate insuranceDueOn) {

    public AssetInput input() {
        return new AssetInput(
                code,
                typeId,
                name,
                departmentId,
                source,
                sourceDescription,
                procurementRequestId,
                acquisitionCost,
                inServiceOn,
                maintenanceIntervalDays,
                maintenanceDueOn,
                licensePlate,
                seatCount,
                vehicleInspectionDueOn,
                insuranceDueOn);
    }
}
