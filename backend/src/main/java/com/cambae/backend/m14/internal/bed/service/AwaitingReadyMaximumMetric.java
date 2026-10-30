package com.cambae.backend.m14.internal.bed.service;

import com.cambae.backend.m14.internal.bed.service.AwaitingReadyRecords.AwaitingReadyRecord;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Chỉ tiêu {@code BED.AWAITING_READY_MAX} (FR-033): thời gian dài nhất (phút) của một lần Chờ sẵn sàng, theo tầng. */
@Component
@Transactional(readOnly = true)
public class AwaitingReadyMaximumMetric implements MetricSource<AwaitingReadyRecord> {

    public static final String CODE = "BED.AWAITING_READY_MAX";

    private static final Metric<AwaitingReadyRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BED_AWAITING_READY,
            "Thời gian giường Chờ sẵn sàng, dài nhất (phút)",
            "Thời gian dài nhất của một lần giường vào Chờ sẵn sàng có lúc bắt đầu trong khoảng; lần còn mở tính tới lúc"
                    + " xem",
            "003 BedStatisticsApi.awaitingReady",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.MAXIMUM, AwaitingReadyRecord::minutes),
            AwaitingReadyRecord::floorKey,
            AwaitingReadyRecords::row);

    private final AwaitingReadyRecords records;

    public AwaitingReadyMaximumMetric(AwaitingReadyRecords records) {
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
