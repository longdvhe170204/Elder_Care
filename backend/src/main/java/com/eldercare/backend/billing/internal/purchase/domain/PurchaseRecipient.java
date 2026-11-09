package com.eldercare.backend.billing.internal.purchase.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

public record PurchaseRecipient(int round, long familyMemberId) {
    public static final SharedKind<PurchaseRecipient> KIND =
            SharedKind.of("M11.PURCHASE_RECIPIENT", PurchaseRecipient.class);
}
