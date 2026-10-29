package com.eldercare.backend.reporting.internal.staffing.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.HandoverReportView;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code SHIFT.UNCONFIRMED_HANDOVERS} ({@code CA.BAN_GIAO_CHUA_XAC_NHAN}; FR-020): bàn giao Đã lập hoặc Có ý kiến,
 * cộng Bản nháp của ca đang Chờ bàn giao, do 008 xác định ({@code StaffStatisticsApi.openHandovers}); mỗi mục kèm trạng
 * thái và thời gian đã chờ từ giờ kết thúc ca. Số liệu "lúc xem" của dashboard theo phạm vi hiện hành, không thuộc báo
 * cáo nào (FR-011).
 */
@Component
@Transactional(readOnly = true)
public class UnconfirmedHandoversMetric implements MetricSource<UnconfirmedHandoversMetric.HandoverRecord> {

    public static final String CODE = "SHIFT.UNCONFIRMED_HANDOVERS";

    /** Một bàn giao đang mở; không gắn người cao tuổi. */
    public record HandoverRecord(HandoverReportView view, long waitingMinutes) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M09.HANDOVER:" + view.handoverId();
        }

        @Override
        public Instant referenceTime() {
            return null;
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

    private static final Metric<HandoverRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HANDOVERS,
            "Bàn giao chưa xác nhận",
            "Bàn giao ở trạng thái Đã lập hoặc Có ý kiến, cộng các ca đang Chờ bàn giao mà bàn giao còn Bản nháp; kèm"
                    + " trạng thái và thời gian đã chờ từ giờ kết thúc ca",
            "008 StaffStatisticsApi.openHandovers",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status(),
            r -> new DetailRow(
                    r.sourceKey(),
                    "008",
                    "HANDOVER",
                    r.view().status(),
                    null,
                    r.floorAreaId(),
                    null,
                    null,
                    null,
                    null,
                    Map.of("shiftEndsAt", r.view().shiftEndsAt(), "waitingMinutes", r.waitingMinutes()),
                    null,
                    Map.of(),
                    false,
                    false,
                    false));

    private final ObjectProvider<StaffStatisticsApi> statistics;
    private final BusinessTime time;

    public UnconfirmedHandoversMetric(ObjectProvider<StaffStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    @Override
    public Metric<HandoverRecord> metric() {
        return METRIC;
    }

    /** Số liệu "lúc xem": bỏ qua khoảng. */
    @Override
    public Stream<HandoverRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        StaffStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Instant now = time.now();
        return api.openHandovers(rough)
                .map(v -> new HandoverRecord(
                        v, Math.max(0, Duration.between(v.shiftEndsAt(), now).toMinutes())));
    }
}
