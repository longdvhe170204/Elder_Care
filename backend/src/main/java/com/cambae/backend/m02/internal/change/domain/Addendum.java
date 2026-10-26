package com.cambae.backend.m02.internal.change.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Phụ lục hợp đồng (data-model mục 7; FR-042, FR-047, DBR-07). {@code contentBefore}, {@code contentAfter} chỉ giữ các
 * thuộc tính bị đổi; {@code contentVersionId} có khi {@code APPLIED}.
 */
public record Addendum(
        long id,
        long contractId,
        long approvalRequestId,
        Set<ChangeType> changeTypes,
        JsonNode contentBefore,
        JsonNode contentAfter,
        LocalDate effectiveDate,
        Status status,
        Long contentVersionId,
        Instant createdAt) {

    public enum Status {
        PENDING_EFFECTIVE,
        APPLIED,
        NOT_APPLIED
    }
}
