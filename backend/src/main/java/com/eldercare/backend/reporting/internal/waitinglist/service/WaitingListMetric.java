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
 * Chỉ tiêu {@code RES.WAITING_LIST} ({@code NCT.DANH_SACH_CHO}; FR-025): số hồ sơ chờ có ngày đăng ký trong khoảng, theo
 * trạng thái hồ sơ chờ của 004; loại lưu trú, mức chăm sóc dự kiến ở dòng chi tiết, lọc được theo loại lưu trú.
 */
@Component
@Transactional(readOnly = true)
public class WaitingListMetric implements MetricSource<WaitingRecord> {

    public static final String CODE = "RES.WAITING_LIST";

    private static final Metric<WaitingRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.WAITING_LIST,
            "Hồ sơ danh sách chờ",
            "Số hồ sơ chờ có ngày đăng ký trong khoảng, chia theo trạng thái hồ sơ chờ hiện tại",
            "004 ResidencyStatisticsApi.waitingList",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status(),
            WaitingListRecords::row);

    private final WaitingListRecords records;

    public WaitingListMetric(WaitingListRecords records) {
        this.records = records;
    }

    @Override
    public Metric<WaitingRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<WaitingRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.registeredIn(from, to, filters);
    }
}
