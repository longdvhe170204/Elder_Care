package com.eldercare.backend.reporting.internal.bed.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.bed.service.BedStateRecords.BedStateRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code BED.USABLE} (FR-020): "tổng giường có thể dùng", không tính giường Không sử dụng và Tạm ngừng sử dụng
 * (Q-246).
 */
@Component
@Transactional(readOnly = true)
public class BedUsableMetric implements MetricSource<BedStateRecord> {

    public static final String CODE = "BED.USABLE";

    private static final Metric<BedStateRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BEDS,
            "Tổng giường có thể dùng",
            "Số giường trừ giường Không sử dụng và Tạm ngừng sử dụng, tại thời điểm xem hoặc cuối ngày đã chọn",
            "003 BedStatisticsApi.stateAt",
            new Metric.Computation.Count<>(
                    r -> r.view().state() != BedStatus.RETIRED && r.view().state() != BedStatus.SUSPENDED),
            null,
            BedStateRecords::row);

    private final BedStateRecords records;

    public BedUsableMetric(BedStateRecords records) {
        this.records = records;
    }

    @Override
    public Metric<BedStateRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<BedStateRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.at(rough, to);
    }
}
