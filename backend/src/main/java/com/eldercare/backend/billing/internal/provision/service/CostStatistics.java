package com.eldercare.backend.billing.internal.provision.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.CostStatisticsApi;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.period.persistence.*;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.ResidentStatusAtDateView;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CostStatistics implements CostStatisticsApi {
    private final CostProvisionStore store;
    private final CostStatementStore statements;
    private final CostPeriodStore periods;
    private final ResidentStatisticsApi residents;

    public CostStatistics(
            CostProvisionStore store,
            CostStatementStore statements,
            CostPeriodStore periods,
            ResidentStatisticsApi residents) {
        this.store = store;
        this.statements = statements;
        this.periods = periods;
        this.residents = residents;
    }

    public Stream<CostItemReportView> items(ScopeFilter scope, LocalDate from, LocalDate to) {
        Map<LocalDate, Set<Long>> byDate = new HashMap<>();
        return store.reports(from, to).stream()
                .filter(i -> scope.facilityWide()
                        || scope.residentIds().contains(i.residentId())
                        || byDate.computeIfAbsent(
                                        i.incurredOn(),
                                        d -> residents
                                                .statusAtDate(scope, d)
                                                .map(ResidentStatusAtDateView::residentId)
                                                .collect(Collectors.toSet()))
                                .contains(i.residentId()))
                .toList()
                .stream();
    }

    public Stream<CostStatementReportView> statements(Optional<Long> period) {
        var due = periods.views().stream()
                .collect(Collectors.toMap(CostPeriodView::id, p -> Optional.ofNullable(p.finalizationDueOn())));
        return statements.list(period.orElse(null), null, null).stream()
                .map(s -> new CostStatementReportView(
                        s.id(),
                        s.residentId(),
                        s.periodId(),
                        CostStatementType.valueOf(s.statementType()),
                        s.finalPeriod(),
                        s.status(),
                        due.getOrDefault(s.periodId(), Optional.empty()).orElse(null)))
                .toList()
                .stream();
    }

    public Stream<CostPeriodView> periods() {
        return periods.views().stream();
    }
}
