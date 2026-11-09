package com.eldercare.backend.billing.internal.item.domain;

import com.eldercare.backend.billing.api.dto.CostType;
import java.util.Set;

public record CostItemRecord(
        long id,
        long statementId,
        long residentId,
        long periodId,
        CostType costType,
        String origin,
        String status,
        Long amount,
        Set<String> flags,
        Long checkedBy,
        long createdBy,
        Long originalPeriodId) {}
