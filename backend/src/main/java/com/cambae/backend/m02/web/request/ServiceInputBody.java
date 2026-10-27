package com.cambae.backend.m02.web.request;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.api.dto.ServiceType;
import com.cambae.backend.m02.internal.servicecatalog.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Thân tạo, sửa dịch vụ (contracts/api-004.yaml, schema {@code ServiceInput}; FR-034). */
@Schema(name = "ServiceInput")
public record ServiceInputBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ServiceType type,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String unit,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        BillingPeriod billingPeriod,

        CareLevel careLevelCode,
        Long roomTypeId,
        Long medicationId) {

    public ServiceCatalogService.Input toInput() {
        return new ServiceCatalogService.Input(
                type, name, unit, billingPeriod, careLevelCode, roomTypeId, medicationId);
    }
}
