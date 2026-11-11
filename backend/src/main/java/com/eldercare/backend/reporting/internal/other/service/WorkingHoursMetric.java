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
import com.eldercare.backend.staffing.api.dto.WorkingHoursView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code SHIFT.WORKING_HOURS} ({@code CA.GIO_LAM}; FR-073): giờ đã làm trong tháng của khoảng (tháng chứa ngày đầu khoảng,
 * xem lúc này thì tháng hiện tại) theo nhân viên; giờ đã xếp còn lại ở danh sách chi tiết (Q-180). Chỉ Quản lý viện xuất
 * file ({@code ExportContent.MANAGER_ONLY}). Nguồn 015 lọc nhân viên theo phạm vi người xem; view chưa có tầng nên dòng
 * không gắn tầng (người xem toàn viện thấy).
 */
@Component
@Transactional(readOnly = true)
public class WorkingHoursMetric implements MetricSource<WorkingHoursMetric.HoursRecord> {

    public static final String CODE = "SHIFT.WORKING_HOURS";

    public record HoursRecord(WorkingHoursView view) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M09.WORKING_HOURS:" + view.staffId() + ":" + view.month();
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }

        BigDecimal workedHours() {
            return BigDecimal.valueOf(view.workedMinutes()).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        }
    }

    private static final Metric<HoursRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.STAFFING_SHIFTS,
            "Số giờ làm trong tháng",
            "Giờ đã làm trong tháng theo nhân viên; giờ đã xếp còn lại tách riêng ở danh sách chi tiết",
            "015 SwapStatisticsApi.workingHoursByMonth",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.SUM, HoursRecord::workedHours),
            r -> Long.toString(r.view().staffId()),
            r -> DetailRow.simple(
                    r.sourceKey(),
                    "015",
                    "WORKING_HOURS",
                    null,
                    null,
                    null,
                    null,
                    Map.of(
                            "staffId", r.view().staffId(),
                            "month", r.view().month().toString(),
                            "workedMinutes", r.view().workedMinutes(),
                            "scheduledMinutes", r.view().scheduledMinutes())));

    private final ObjectProvider<SwapStatisticsApi> statistics;
    private final BusinessTime time;

    public WorkingHoursMetric(ObjectProvider<SwapStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    @Override
    public Metric<HoursRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<HoursRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        SwapStatisticsApi api = statistics.getIfAvailable();
        if (api == null) return Stream.empty();
        YearMonth month = from == null ? YearMonth.from(time.today()) : YearMonth.from(from.atZone(BusinessTime.ZONE));
        return api.workingHoursByMonth(rough, month)
                .filter(v -> filters.staffId() == null || filters.staffId().equals(v.staffId()))
                .map(HoursRecord::new);
    }
}
