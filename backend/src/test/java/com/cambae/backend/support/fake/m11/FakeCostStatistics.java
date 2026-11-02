package com.cambae.backend.support.fake.m11;

import com.cambae.backend.m11.api.CostStatisticsApi;
import com.cambae.backend.m11.api.dto.*;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/** Fixture đặt dòng đã thuộc từng phạm vi; không mô phỏng nguồn vị trí hoặc kiểm quyền thật. */
public class FakeCostStatistics implements CostStatisticsApi {
    private final Map<ScopeFilter, List<CostItemReportView>> rows = new HashMap<>();
    private List<CostStatementReportView> statements = List.of();
    private List<CostPeriodView> periods = List.of();

    public FakeCostStatistics items(ScopeFilter scope, List<CostItemReportView> items) {
        rows.put(scope, List.copyOf(items));
        return this;
    }

    public FakeCostStatistics statements(List<CostStatementReportView> items) {
        statements = List.copyOf(items);
        return this;
    }

    public FakeCostStatistics periods(List<CostPeriodView> items) {
        periods = List.copyOf(items);
        return this;
    }

    @Override
    public Stream<CostItemReportView> items(ScopeFilter scope, LocalDate from, LocalDate to) {
        return rows.getOrDefault(scope, List.of()).stream()
                .filter(r -> !r.incurredOn().isBefore(from) && !r.incurredOn().isAfter(to));
    }

    @Override
    public Stream<CostStatementReportView> statements(Optional<Long> periodId) {
        return statements.stream().filter(r -> periodId.isEmpty() || r.periodId() == periodId.get());
    }

    @Override
    public Stream<CostPeriodView> periods() {
        return periods.stream();
    }

    public void reset() {
        rows.clear();
        statements = List.of();
        periods = List.of();
    }
}
