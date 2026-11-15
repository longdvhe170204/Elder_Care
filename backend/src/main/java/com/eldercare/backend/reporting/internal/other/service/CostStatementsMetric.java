package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.CostStatisticsApi;
import com.eldercare.backend.billing.api.dto.CostStatementReportView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code COST.STATEMENTS} ({@code CP.BANG}; FR-020, FR-051): bảng chi phí thường và bổ sung của các kỳ giao khoảng (xem lúc
 * này: mọi kỳ), chia theo trạng thái; cho biết số bảng chưa chốt (010 FR-002a).
 */
@Component
@Transactional(readOnly = true)
public class CostStatementsMetric implements MetricSource<CostStatementsMetric.StatementRecord> {

    public static final String CODE = "COST.STATEMENTS";

    public record StatementRecord(CostStatementReportView view) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M11.COST_STATEMENT:" + view.id();
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }
    }

    private static final Metric<StatementRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.INCURRED_COSTS,
            "Bảng chi phí",
            "Bảng chi phí thường và bổ sung của các kỳ giao khoảng, chia theo trạng thái",
            "010 CostStatisticsApi.statements",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status(),
            r -> DetailRow.simple(
                    r.sourceKey(),
                    "010",
                    r.view().statementType().name(),
                    r.view().status(),
                    null,
                    null,
                    r.residentId(),
                    Map.of(
                            "periodId",
                            r.view().periodId(),
                            "finalPeriod",
                            r.view().finalPeriod())));

    private final ObjectProvider<CostStatisticsApi> statistics;

    public CostStatementsMetric(ObjectProvider<CostStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<StatementRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<StatementRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        CostStatisticsApi api = statistics.getIfAvailable();
        if (api == null) return Stream.empty();
        LocalDate start = from == null ? null : from.atZone(BusinessTime.ZONE).toLocalDate();
        LocalDate end =
                to == null ? null : to.minusNanos(1).atZone(BusinessTime.ZONE).toLocalDate();
        return api.periods()
                .filter(p -> start == null
                        || end == null
                        || (!p.throughDate().isBefore(start) && !p.fromDate().isAfter(end)))
                .flatMap(p -> api.statements(Optional.of(p.id())))
                .filter(s -> !"CANCELLED".equals(s.status()))
                .filter(s -> rough.facilityWide() || rough.residentIds().contains(s.residentId()))
                .map(StatementRecord::new);
    }
}
