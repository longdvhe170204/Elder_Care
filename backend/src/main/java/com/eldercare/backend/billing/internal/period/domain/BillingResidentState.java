package com.eldercare.backend.billing.internal.period.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/** Trạng thái M11 theo người: mốc hoàn tất chi phí kỳ cuối sau qua đời (FR-036). */
public record BillingResidentState(Instant finalCostCompletedFrom) {
    public static final SharedKind<BillingResidentState> KIND =
            SharedKind.of("M11.RESIDENT_COST_STATE", BillingResidentState.class);

    public static BillingResidentState empty() {
        return new BillingResidentState(null);
    }
}
