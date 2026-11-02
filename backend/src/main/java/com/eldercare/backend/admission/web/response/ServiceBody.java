package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.internal.servicecatalog.domain.ServiceRecord;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dịch vụ trong danh mục (contracts/api-004.yaml, schema {@code Service}; FR-034). {@code version} cũng là ETag của lệnh
 * sửa, ngừng hiệu lực.
 */
@Schema(name = "Service")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceBody(
        long id,
        ServiceType type,
        String name,
        String unit,
        BillingPeriod billingPeriod,
        CareLevel careLevelCode,
        Long roomTypeId,
        Long medicationId,
        String status,
        long version) {

    public static ServiceBody of(ServiceRecord service) {
        return new ServiceBody(
                service.id(),
                service.type(),
                service.name(),
                service.unit(),
                service.billingPeriod(),
                service.careLevel(),
                service.roomTypeId(),
                service.medicationId(),
                service.active() ? "ACTIVE" : "INACTIVE",
                service.version());
    }
}
