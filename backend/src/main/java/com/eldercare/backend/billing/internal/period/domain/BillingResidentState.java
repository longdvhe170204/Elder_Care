package com.eldercare.backend.billing.internal.period.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.time.LocalDate;

/** Trạng thái M11 theo người: mốc kỳ cuối hiện hành và hoàn tất sau qua đời (FR-034/036). */
public record BillingResidentState(
        Instant finalCostCompletedFrom, Long finalStatementId, LocalDate throughDate, Instant deathAt, long revision) {
    public BillingResidentState(Instant finalCostCompletedFrom) {
        this(finalCostCompletedFrom, null, null, null, 0);
    }

    public static final SharedKind<BillingResidentState> KIND =
            SharedKind.of("M11.RESIDENT_COST_STATE", BillingResidentState.class);

    public static BillingResidentState empty() {
        return new BillingResidentState(null);
    }
}
