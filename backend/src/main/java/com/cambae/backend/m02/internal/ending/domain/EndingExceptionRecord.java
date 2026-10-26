package com.cambae.backend.m02.internal.ending.domain;

import java.time.Instant;

/**
 * Ngoại lệ điều kiện kết thúc đã duyệt (nhóm 3; FR-064). {@code endingId} rỗng khi căn cứ là danh sách việc sau qua đời
 * (FR-071a, Q-387).
 */
public record EndingExceptionRecord(
        long id,
        Long endingId,
        long residentId,
        EndingConditionCode conditionCode,
        long approvalRequestId,
        Instant createdAt) {}
