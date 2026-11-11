package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.CostStatisticsApi;
import com.eldercare.backend.billing.api.dto.CostItemReportView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters.CostMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khoản chi phí của 010 cho các chỉ tiêu {@code COST.*} (FR-050 → FR-053): bỏ khoản Đã hủy; chế độ đã chốt (mặc định) chỉ
 * khoản Đã chốt, tạm tính thêm khoản chưa hủy của bảng chưa chốt (FR-051, Q-136). Khoản "thiếu đơn giá" không có thành
 * tiền nên không vào tổng. Thuốc chỉ có mã vật phẩm (Q-133, nguồn đã che).
 */
@Component
@Transactional(readOnly = true)
public class CostItems {

    public record CostRecord(CostItemReportView view) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M11.COST_ITEM:" + view.id();
        }

        @Override
        public Instant referenceTime() {
            return view.incurredOn() == null
                    ? null
                    : view.incurredOn().atStartOfDay(BusinessTime.ZONE).toInstant();
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }

        public BigDecimal amount() {
            return view.flags().contains("MISSING_UNIT_PRICE") || view.amount() == null
                    ? null
                    : BigDecimal.valueOf(view.amount());
        }
    }

    private final ObjectProvider<CostStatisticsApi> statistics;

    public CostItems(ObjectProvider<CostStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    public Stream<CostRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        CostStatisticsApi api = statistics.getIfAvailable();
        if (api == null || from == null || to == null) return Stream.empty();
        boolean provisional = filters.costMode() == CostMode.PROVISIONAL;
        return api.items(
                        rough,
                        from.atZone(BusinessTime.ZONE).toLocalDate(),
                        to.minusNanos(1).atZone(BusinessTime.ZONE).toLocalDate())
                .filter(i -> !"CANCELLED".equals(i.status()))
                .filter(i -> provisional || "FINALIZED".equals(i.status()))
                .map(CostRecord::new);
    }

    static DetailRow row(CostRecord r) {
        var result = new LinkedHashMap<String, Object>();
        result.put("costType", r.view().costType().name());
        result.put("origin", r.view().origin());
        result.put("itemCode", r.view().itemCode());
        result.put("quantity", r.view().quantity());
        result.put("amount", r.view().amount());
        if (r.view().originalPeriod() != null)
            result.put("originalPeriod", r.view().originalPeriod());
        return DetailRow.simple(
                r.sourceKey(), "010", "COST_ITEM", r.view().status(), r.referenceTime(), null, r.residentId(), result);
    }
}
