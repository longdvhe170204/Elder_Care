package com.eldercare.backend.reporting.internal.waitinglist.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.waitinglist.service.WaitingListRecords.WaitingRecord;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code RES.WAITING_TIME} (FR-025: thời gian chờ của hồ sơ đang chờ): số liệu "lúc xem", bỏ qua khoảng vì trạng
 * thái hồ sơ chờ là trạng thái hiện tại của 004.
 */
@Component
@Transactional(readOnly = true)
public class WaitingTimeMetric implements MetricSource<WaitingRecord> {

    public static final String CODE = "RES.WAITING_TIME";

    private static final Metric<WaitingRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.WAITING_LIST,
            "Thời gian chờ trung bình (ngày)",
            "Trung bình số ngày từ ngày đăng ký tới hôm nay của các hồ sơ đang chờ (Chờ, Đã liên hệ) lúc xem",
            "004 ResidencyStatisticsApi.waitingList",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.AVERAGE, WaitingRecord::waitingDays),
            r -> r.view().residenceType().name(),
            WaitingListRecords::row);

    private final WaitingListRecords records;

    public WaitingTimeMetric(WaitingListRecords records) {
        this.records = records;
    }

    @Override
    public Metric<WaitingRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<WaitingRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.openNow(filters);
    }
}
