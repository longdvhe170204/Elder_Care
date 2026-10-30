package com.eldercare.backend.notification.internal.urgent.domain;

import java.time.Instant;

/** Một kết quả gọi của lượt (FR-030, FR-031). */
public record CallResultRow(
        long id,
        long calledBy,
        Instant calledAt,
        String calledPhoneNumber,
        String result,
        String reason,
        String answeredBy,
        String note,
        boolean supplementary) {}
