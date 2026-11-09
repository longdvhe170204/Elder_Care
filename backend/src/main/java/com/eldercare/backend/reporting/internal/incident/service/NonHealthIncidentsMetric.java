package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.incident.service.IncidentRecords.IncidentRecord;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.NON_HEALTH_INCIDENTS} ({@code SK.SU_CO_KHONG_THUOC_SK}; FR-042a, Q-202): sự cố thuộc loại đang
 * mang dấu "không thuộc sức khỏe" trên danh mục hiện hành của 007 (sự cố đổi sang loại không mang dấu rời khỏi số từ
 * lần lấy kế tiếp, Q-73), chia theo loại. Dòng chỉ có người cao tuổi, loại, mức, thời điểm; không có kết quả (mô tả, xử
 * lý ban đầu, diễn biến).
 */
@Component
@Transactional(readOnly = true)
public class NonHealthIncidentsMetric implements MetricSource<IncidentRecord> {

    public static final String CODE = "HEALTH.NON_HEALTH_INCIDENTS";

    private static final Metric<IncidentRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.NON_HEALTH_INCIDENTS,
            "Sự cố không thuộc sức khỏe",
            "Số sự cố tạo trong khoảng thuộc loại đang mang dấu \"không thuộc sức khỏe\", không gồm Đã hủy, chia theo"
                    + " loại",
            "007 IncidentStatisticsApi.incidents (nonHealth)",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().type(),
            NonHealthIncidentsMetric::row);

    private final IncidentRecords records;

    public NonHealthIncidentsMetric(IncidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<IncidentRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<IncidentRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.view().nonHealth());
    }

    private static DetailRow row(IncidentRecord r) {
        return new DetailRow(
                r.sourceKey(),
                "007",
                r.view().type(),
                r.view().level(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                r.residentId(),
                null,
                null,
                Map.of(),
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
