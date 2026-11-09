package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import java.time.LocalDate;
import java.util.*;

/** Bản giả duy nhất của port 010, chỉ dùng trong test của 017. */
public class FakeCostForBalance implements CostForBalancePort {
    private final Map<Long, Long> totals = new HashMap<>();
    private final Map<Day, Long> fees = new HashMap<>();
    private final Map<Long, com.eldercare.backend.billing.internal.provision.domain.FinalizedCostStatement> statements =
            new HashMap<>();

    public FakeCostForBalance statement(
            com.eldercare.backend.billing.internal.provision.domain.FinalizedCostStatement value) {
        statements.put(value.statement().id(), value);
        return this;
    }

    @Override
    public Optional<com.eldercare.backend.billing.internal.provision.domain.FinalizedCostStatement> finalizedStatement(
            long id) {
        return Optional.ofNullable(statements.get(id));
    }

    private record Day(long residentId, LocalDate date) {}

    public FakeCostForBalance total(long residentId, long amount) {
        totals.put(residentId, amount);
        return this;
    }

    public FakeCostForBalance dailyFee(long residentId, LocalDate date, long amount) {
        fees.put(new Day(residentId, date), amount);
        return this;
    }

    @Override
    public long unfinalizedTotal(long residentId) {
        return totals.getOrDefault(residentId, 0L);
    }

    @Override
    public Optional<Long> baseDailyFee(long residentId, LocalDate date) {
        return Optional.ofNullable(fees.get(new Day(residentId, date)));
    }

    public void reset() {
        totals.clear();
        fees.clear();
        statements.clear();
    }
}
