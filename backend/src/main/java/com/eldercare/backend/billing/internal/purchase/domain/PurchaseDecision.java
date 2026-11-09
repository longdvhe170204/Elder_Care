package com.eldercare.backend.billing.internal.purchase.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

public record PurchaseDecision(
        String type,
        Long previousAmount,
        Long newAmount,
        String actorType,
        long actorId,
        String channel,
        String reason,
        boolean insufficientBalance,
        Instant occurredAt) {
    public static final SharedKind<PurchaseDecision> KIND =
            SharedKind.of("M11.PURCHASE_DECISION", PurchaseDecision.class);
}
