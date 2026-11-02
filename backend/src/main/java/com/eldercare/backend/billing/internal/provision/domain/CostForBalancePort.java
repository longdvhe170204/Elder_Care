package com.eldercare.backend.billing.internal.provision.domain;

import java.time.LocalDate;
import java.util.Optional;

/** P-12/T004: 010 cài, chỉ balance/procurement gọi; tiền nguyên đồng có dấu. */
public interface CostForBalancePort {
    long unfinalizedTotal(long residentId);

    Optional<Long> baseDailyFee(long residentId, LocalDate date);
}
