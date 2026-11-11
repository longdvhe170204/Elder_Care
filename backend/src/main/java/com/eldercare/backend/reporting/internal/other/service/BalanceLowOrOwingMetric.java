package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.BalanceStatisticsApi;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.api.dto.BalanceStatusView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code BALANCE.LOW_OR_OWING} ({@code SD.THAP_CON_NO}; FR-020): người cao tuổi có số dư Sắp hết tiền hoặc Còn nợ lúc xem
 * (017), chia theo tình trạng.
 */
@Component
@Transactional(readOnly = true)
public class BalanceLowOrOwingMetric implements MetricSource<BalanceLowOrOwingMetric.BalanceRecord> {

    public static final String CODE = "BALANCE.LOW_OR_OWING";

    public record BalanceRecord(BalanceStatusView view) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M11.BALANCE:" + view.residentId();
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

    private static final Metric<BalanceRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BALANCES,
            "Số dư sắp hết hoặc còn nợ",
            "Người cao tuổi có số dư Sắp hết tiền hoặc Còn nợ lúc xem, chia theo tình trạng",
            "017 BalanceStatisticsApi.statuses",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status().name(),
            r -> {
                var result = new LinkedHashMap<String, Object>();
                result.put("balance", r.view().balance());
                result.put("daysCovered", r.view().daysCovered());
                return DetailRow.simple(
                        r.sourceKey(), "017", "BALANCE", r.view().status().name(), null, null, r.residentId(), result);
            });

    private final ObjectProvider<BalanceStatisticsApi> statistics;

    public BalanceLowOrOwingMetric(ObjectProvider<BalanceStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<BalanceRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<BalanceRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        BalanceStatisticsApi api = statistics.getIfAvailable();
        if (api == null) return Stream.empty();
        return api.statuses(rough)
                .filter(v -> v.status() != BalanceStatus.NORMAL)
                .map(BalanceRecord::new);
    }
}
