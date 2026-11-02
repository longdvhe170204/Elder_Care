package com.eldercare.backend.admission.internal.servicecatalog.domain;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.resident.api.dto.CareLevel;

/** Dịch vụ trong danh mục (data-model mục 6, bảng {@code service}); {@code careLevel} chỉ với loại CARE (Q-297). */
public record ServiceRecord(
        long id,
        ServiceType type,
        String name,
        String unit,
        BillingPeriod billingPeriod,
        CareLevel careLevel,
        Long roomTypeId,
        Long medicationId,
        boolean active,
        long version) {}
