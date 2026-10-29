package com.eldercare.backend.reporting.internal.bed.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.bed.service.AwaitingReadyRecords.AwaitingReadyRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Chỉ tiêu {@code BED.AWAITING_READY_OVERDUE} (FR-033): số lần Chờ sẵn sàng vượt hạn CFG-M03-04, theo tầng. */
@Component
@Transactional(readOnly = true)
public class AwaitingReadyOverdueMetric implements MetricSource<AwaitingReadyRecord> {

    public static final String CODE = "BED.AWAITING_READY_OVERDUE";

    private static final Metric<AwaitingReadyRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BED_AWAITING_READY,
            "Số lần giường Chờ sẵn sàng vượt hạn",
            "Số lần giường vào Chờ sẵn sàng có lúc bắt đầu trong khoảng mà thời gian vượt CFG-M03-04 (mặc định 4 giờ)",
            "003 BedStatisticsApi.awaitingReady",
            new Metric.Computation.Count<>(AwaitingReadyRecord::overdue),
            AwaitingReadyRecord::floorKey,
            AwaitingReadyRecords::row);

    private final AwaitingReadyRecords records;

    public AwaitingReadyOverdueMetric(AwaitingReadyRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AwaitingReadyRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AwaitingReadyRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to);
    }
}
