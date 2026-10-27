package com.cambae.backend.m02.internal.servicecatalog.domain;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.api.dto.ServiceType;

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
