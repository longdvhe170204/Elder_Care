package com.eldercare.backend.billing.internal.item.domain;

import java.util.List;

public record CostBatchResult(List<Long> succeededIds, List<Skipped> skipped) {
    public CostBatchResult {
        succeededIds = List.copyOf(succeededIds);
        skipped = List.copyOf(skipped);
    }

    public record Skipped(Long itemId, String errorCode, String reason) {}
}
