package com.eldercare.backend.reporting.internal.bed.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.bed.service.BedStateRecords.BedStateRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code BED.STATUS} ({@code GIUONG.TRANG_THAI}; FR-020, FR-024): số giường theo từng trạng thái của bảng trạng
 * thái giường 003, Đang giữ chỗ tách hồ sơ chờ ({@code HELD_WAITING_RECORD}) và người vắng ({@code HELD_ABSENT_RESIDENT}).
 */
@Component
@Transactional(readOnly = true)
public class BedStatusMetric implements MetricSource<BedStateRecord> {

    public static final String CODE = "BED.STATUS";

    private static final Metric<BedStateRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BEDS,
            "Giường theo trạng thái",
            "Số giường theo từng trạng thái của bảng trạng thái giường (003) tại thời điểm xem hoặc cuối ngày đã chọn;"
                    + " Đang giữ chỗ tách hồ sơ chờ và người vắng",
            "003 BedStatisticsApi.stateAt",
            new Metric.Computation.Count<>(r -> true),
            BedStateRecord::key,
            BedStateRecords::row);

    private final BedStateRecords records;

    public BedStatusMetric(BedStateRecords records) {
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
