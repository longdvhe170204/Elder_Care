package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.ProcurementStatisticsApi;
import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code COST.PURCHASING} ({@code CP.MUA_SAM}; FR-055a, Q-284): tổng chi mua sắm của yêu cầu lập trong khoảng (019) theo
 * trạng thái, tách khỏi chi phí của người cao tuổi: thực chi khi đã mua, chưa mua thì số được duyệt, chưa duyệt thì tổng
 * dự kiến. Không cộng vào tổng chi phí phát sinh hay file kế toán của 010.
 */
@Component
@Transactional(readOnly = true)
public class CostPurchasingMetric implements MetricSource<CostPurchasingMetric.PurchaseRecord> {

    public static final String CODE = "COST.PURCHASING";

    public record PurchaseRecord(ProcurementRequestView view) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M11.PROCUREMENT_REQUEST:" + view.requestId();
        }

        @Override
        public Instant referenceTime() {
            return view.createdOn().atStartOfDay(BusinessTime.ZONE).toInstant();
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }

        BigDecimal amount() {
            long value = view.actualAmount() != null
                    ? view.actualAmount()
                    : view.approvedAmount() != null ? view.approvedAmount() : view.estimatedTotal();
            return BigDecimal.valueOf(value);
        }
    }

    private static final Metric<PurchaseRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.PURCHASING,
            "Tổng chi mua sắm",
            "Yêu cầu mua sắm lập trong khoảng theo trạng thái: thực chi khi đã mua, chưa mua thì số được duyệt, chưa duyệt"
                    + " thì tổng dự kiến; tách khỏi chi phí của người cao tuổi",
            "019 ProcurementStatisticsApi.byDepartment",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.SUM, PurchaseRecord::amount),
            r -> r.view().status().name(),
            r -> DetailRow.simple(
                    r.sourceKey(),
                    "019",
                    r.view().type().name(),
                    r.view().status().name(),
                    r.referenceTime(),
                    null,
                    null,
                    Map.of("departmentId", r.view().departmentId(), "amount", r.amount())));

    private final ObjectProvider<ProcurementStatisticsApi> statistics;

    public CostPurchasingMetric(ObjectProvider<ProcurementStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<PurchaseRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<PurchaseRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        ProcurementStatisticsApi api = statistics.getIfAvailable();
        if (api == null || from == null || to == null) return Stream.empty();
        return api.byDepartment(
                        Set.of(),
                        from.atZone(BusinessTime.ZONE).toLocalDate(),
                        to.minusNanos(1).atZone(BusinessTime.ZONE).toLocalDate())
                .map(PurchaseRecord::new);
    }
}
