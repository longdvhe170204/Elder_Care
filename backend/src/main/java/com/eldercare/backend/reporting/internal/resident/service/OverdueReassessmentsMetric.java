package com.eldercare.backend.reporting.internal.resident.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.OverdueReassessmentView;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.OVERDUE_REASSESSMENTS} ({@code SK.DANH_GIA_LAI_QUA_HAN}; FR-044, Q-307): người cao tuổi đang ở
 * trạng thái "quá hạn đánh giá lại" do 001 xác định (CFG-M01-03), số lượng và danh sách. Số liệu "lúc xem"; tầng, phòng
 * theo nơi ở hiện tại.
 */
@Component
@Transactional(readOnly = true)
public class OverdueReassessmentsMetric implements MetricSource<OverdueReassessmentsMetric.OverdueRecord> {

    public static final String CODE = "HEALTH.OVERDUE_REASSESSMENTS";

    /** Một người cao tuổi quá hạn đánh giá lại. */
    public record OverdueRecord(OverdueReassessmentView view, Long floorAreaId, Long roomId) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M01.OVERDUE_REASSESSMENT:" + view.residentId();
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }
    }

    private static final Metric<OverdueRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.OVERDUE_REASSESSMENTS,
            "Quá hạn đánh giá lại",
            "Người cao tuổi đang quá hạn đánh giá lại (quá CFG-M01-03 kể từ lúc hệ thống báo cần đánh giá lại mà chưa"
                    + " có lần đánh giá lại Đã xác nhận) lúc xem",
            "001 ResidentStatisticsApi.overdueReassessments",
            new Metric.Computation.Count<>(r -> true),
            null,
            r -> new DetailRow(
                    r.sourceKey(),
                    "001",
                    "OVERDUE_REASSESSMENT",
                    null,
                    null,
                    r.floorAreaId(),
                    null,
                    r.residentId(),
                    null,
                    null,
                    Map.of("overdueSince", r.view().overdueSince()),
                    InfoGroup.HEALTH,
                    Map.of(),
                    false,
                    false,
                    false));

    private final ObjectProvider<ResidentStatisticsApi> statistics;
    private final ObjectProvider<ResidentLocationApi> locations;

    public OverdueReassessmentsMetric(
            ObjectProvider<ResidentStatisticsApi> statistics, ObjectProvider<ResidentLocationApi> locations) {
        this.statistics = statistics;
        this.locations = locations;
    }

    @Override
    public Metric<OverdueRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<OverdueRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        ResidentStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        List<OverdueReassessmentView> overdue = api.overdueReassessments(rough).toList();
        Map<Long, LocationView> at = CurrentLocations.of(
                locations,
                overdue.stream().map(OverdueReassessmentView::residentId).toList());
        return overdue.stream().map(v -> {
            LocationView l = at.get(v.residentId());
            return new OverdueRecord(v, l == null ? null : l.floorId(), l == null ? null : l.roomId());
        });
    }
}
