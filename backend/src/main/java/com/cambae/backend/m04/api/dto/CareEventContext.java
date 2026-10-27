package com.cambae.backend.m04.api.dto;

import com.cambae.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.*;

/** Metadata kỹ thuật chống lặp; milestone là mốc nghiệp vụ ổn định, không dùng giờ phát lại. */
public record CareEventContext(
        String sourceType,
        long sourceId,
        String milestone,
        Instant occurredAt,
        Optional<Long> floorAreaId,
        List<RecipientGroup> recipients) {
    public CareEventContext {
        Objects.requireNonNull(sourceType);
        Objects.requireNonNull(milestone);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(floorAreaId);
        recipients = List.copyOf(recipients);
    }
}
