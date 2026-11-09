package com.eldercare.backend.billing.internal.source.domain;

import java.time.LocalDate;
import java.util.List;

public record SourceCostSelection(
        long residentId,
        String sourceType,
        List<String> sourceIds,
        List<BillingSourceKey> sourceUnits,
        LocalDate from,
        LocalDate to) {}
