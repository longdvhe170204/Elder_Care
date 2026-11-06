package com.eldercare.backend.reporting.internal.medication.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.medication.api.MedicationStatisticsApi;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code MED.OVERDUE_RECONCILIATIONS} ({@code THUOC.DOI_CHIEU_QUA_HAN}; FR-044, 006 FR-051): phiếu đối chiếu Mở
 * hoặc Chờ xác nhận đang mang dấu "quá hạn" do 006 xác định. Số liệu "lúc xem": đọc mọi phiếu, bỏ qua khoảng.
 */
@Component
@Transactional(readOnly = true)
public class OverdueReconciliationsMetric implements MetricSource<OverdueReconciliationsMetric.ReconciliationRecord> {

    public static final String CODE = "MED.OVERDUE_RECONCILIATIONS";

    private static final Set<String> NOT_CLOSED = Set.of("OPEN", "PENDING_CONFIRMATION");

    /** Một phiếu đối chiếu; không gắn tầng (006 chỉ trả người cao tuổi). */
    public record ReconciliationRecord(ReconciliationReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M07.RECONCILIATION:" + view.reconciliationId();
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

        boolean overdue() {
            return view.isOverdue() && NOT_CLOSED.contains(view.status());
        }
    }

    private static final Metric<ReconciliationRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.MEDICATION_RECONCILIATIONS,
            "Phiếu đối chiếu thuốc quá hạn",
            "Phiếu đối chiếu Mở hoặc Chờ xác nhận đang mang dấu quá hạn (CFG-M07-05) lúc xem",
            "006 MedicationStatisticsApi.reconciliationsInPeriod",
            new Metric.Computation.Count<>(ReconciliationRecord::overdue),
            r -> r.view().status(),
            r -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("reason", r.view().reason());
                result.put("deadline", r.view().deadline());
                return new DetailRow(
                        r.sourceKey(),
                        "006",
                        "RECONCILIATION",
                        r.view().status(),
                        null,
                        null,
                        null,
                        r.residentId(),
                        null,
                        null,
                        result,
                        InfoGroup.MEDICATION,
                        Map.of(),
                        false,
                        false,
                        false);
            });

    private final ObjectProvider<MedicationStatisticsApi> statistics;

    public OverdueReconciliationsMetric(ObjectProvider<MedicationStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<ReconciliationRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<ReconciliationRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        MedicationStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        // 006 lọc theo lúc lập phiếu; phiếu đang quá hạn có thể lập từ trước khoảng nên đọc mọi lúc lập.
        return api.reconciliationsInPeriod(rough, Instant.EPOCH, Instant.MAX).map(ReconciliationRecord::new);
    }
}
