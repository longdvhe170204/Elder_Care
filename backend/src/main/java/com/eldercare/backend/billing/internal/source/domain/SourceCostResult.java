package com.eldercare.backend.billing.internal.source.domain;

import java.util.List;

public record SourceCostResult(List<Long> succeededIds, List<Skipped> skipped) {
    public record Skipped(String source, String errorCode, String reason) {}
}
