package com.eldercare.backend.reporting.internal.quality.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.dto.OutOfCheckScopeView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code QC.OUT_OF_CHECK_SCOPE} ({@code KT.NGOAI_PHAM_VI_KIEM_TRA}; FR-037, Q-168, Q-254): công việc do Trưởng tầng
 * được giao, hoặc Người phụ trách ca của ca không có Trưởng tầng được giao, tự thực hiện nên không vào mẫu kiểm tra; chia
 * theo nhân viên thực hiện. Không có tỷ lệ Đạt cho các công việc này ({@code QC.PASS_RATE} chỉ đọc mục đã kiểm tra).
 */
@Component
@Transactional(readOnly = true)
public class OutOfCheckScopeMetric implements MetricSource<OutOfCheckScopeMetric.OutOfScopeRecord> {

    public static final String CODE = "QC.OUT_OF_CHECK_SCOPE";

    /** Một công việc ngoài phạm vi kiểm tra; mốc xếp khoảng là lúc thực hiện. */
    public record OutOfScopeRecord(OutOfCheckScopeView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.CARE_TASK:" + view.careTaskId();
        }

        @Override
        public Instant referenceTime() {
            return view.performedAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<OutOfScopeRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.QUALITY_CHECKS,
            "Ngoài phạm vi kiểm tra",
            "Công việc do chính người kiểm tra (Trưởng tầng được giao, hoặc Người phụ trách ca khi ca không có Trưởng tầng"
                    + " được giao) thực hiện nên không vào mẫu kiểm tra, chia theo nhân viên thực hiện; không tính tỷ lệ Đạt",
            "014 ActivityStatisticsApi.outOfCheckScope",
            new Metric.Computation.Count<>(r -> true),
            r -> Long.toString(r.view().performerStaffId()),
            r -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("careTaskId", r.view().careTaskId());
                result.put("shiftId", r.view().shiftId());
                result.put("performerRole", r.view().performerRole());
                return new DetailRow(
                        r.sourceKey(),
                        "014",
                        "CARE_TASK",
                        "OUT_OF_CHECK_SCOPE",
                        r.referenceTime(),
                        r.floorAreaId(),
                        null,
                        null,
                        null,
                        Long.toString(r.view().performerStaffId()),
                        result,
                        null,
                        Map.of(),
                        false,
                        false,
                        false);
            });

    private final QualityCheckRecords records;

    public OutOfCheckScopeMetric(QualityCheckRecords records) {
        this.records = records;
    }

    @Override
    public Metric<OutOfScopeRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<OutOfScopeRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.outOfScope(rough, from, to).map(OutOfScopeRecord::new);
    }
}
