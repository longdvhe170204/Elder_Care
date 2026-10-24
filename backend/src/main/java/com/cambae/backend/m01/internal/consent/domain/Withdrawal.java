package com.cambae.backend.m01.internal.consent.domain;

import java.time.Instant;

/**
 * Việc chuyển sang Đã rút lại (FR-013, FR-014). Rút lại bằng lệnh có {@code reason} và {@code evidenceFileId}; được thay
 * thế bởi bản mới thì có {@code supersededById} và không có hai trường kia.
 */
public record Withdrawal(Instant at, long by, String reason, Long evidenceFileId, Long supersededById) {}
