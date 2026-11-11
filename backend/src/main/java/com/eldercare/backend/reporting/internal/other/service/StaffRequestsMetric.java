package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.staffing.api.SwapStatisticsApi;
import com.eldercare.backend.staffing.api.dto.StaffRequestView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code SHIFT.REQUESTS} ({@code CA.YEU_CAU}; FR-072): yêu cầu đổi ca, nghỉ đột xuất lập trong khoảng theo loại và trạng
 * thái (015); không có lý do, không chia theo loại nghỉ để không lộ thông tin sức khỏe của nhân viên.
 */
@Component
@Transactional(readOnly = true)
public class StaffRequestsMetric implements MetricSource<StaffRequestsMetric.RequestRecord> {

    public static final String CODE = "SHIFT.REQUESTS";

    public record RequestRecord(StaffRequestView view) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M09.STAFF_REQUEST:" + view.requestId();
        }

        @Override
        public Instant referenceTime() {
            return view.filedAt();
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

    private static final Metric<RequestRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.STAFFING_SHIFTS,
            "Yêu cầu đổi ca, nghỉ đột xuất",
            "Yêu cầu lập trong khoảng, chia theo loại và trạng thái; không có lý do, không chia theo loại nghỉ",
            "015 SwapStatisticsApi.requests",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().type() + ":" + r.view().status(),
            r -> {
                var result = new LinkedHashMap<String, Object>();
                result.put("type", r.view().type());
                result.put("decidedAt", r.view().decidedAt());
                return DetailRow.simple(
                        r.sourceKey(),
                        "015",
                        r.view().type(),
                        r.view().status(),
                        r.view().filedAt(),
                        r.floorAreaId(),
                        null,
                        result);
            });

    private final ObjectProvider<SwapStatisticsApi> statistics;

    public StaffRequestsMetric(ObjectProvider<SwapStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<RequestRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<RequestRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        SwapStatisticsApi api = statistics.getIfAvailable();
        if (api == null || from == null || to == null) return Stream.empty();
        return api.requests(
                        rough,
                        from.atZone(BusinessTime.ZONE).toLocalDate(),
                        to.minusNanos(1).atZone(BusinessTime.ZONE).toLocalDate())
                .map(RequestRecord::new);
    }
}
