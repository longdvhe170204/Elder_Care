package com.eldercare.backend.reporting.internal.resident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.AttendanceView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code RES.DAY_CARE_ATTENDANCE} (phần bán trú của {@code NCT.DANG_LUU_TRU}; FR-020, 3.4): người bán trú có
 * lịch tại ngày cuối của khoảng (dashboard: hôm nay), chia theo trạng thái có mặt theo ngày của 005
 * ({@code TaskStatisticsApi.attendanceInRange}). Tầng tại ngày qua {@code ResidentLocationApi.floorOn}.
 */
@Component
@Transactional(readOnly = true)
public class DayCareAttendanceMetric implements MetricSource<DayCareAttendanceMetric.AttendanceRecord> {

    public static final String CODE = "RES.DAY_CARE_ATTENDANCE";

    /** Một lịch bán trú của một người trong ngày; mốc xếp khoảng là đầu ngày. */
    public record AttendanceRecord(AttendanceView view, Long floorAreaId) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.ATTENDANCE:" + view.id();
        }

        @Override
        public Instant referenceTime() {
            return view.date().atStartOfDay(BusinessTime.ZONE).toInstant();
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }
    }

    private static final Metric<AttendanceRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.RESIDENTS,
            "Người bán trú có lịch theo trạng thái có mặt",
            "Số người bán trú có lịch tại ngày cuối của khoảng (dashboard: hôm nay), chia theo trạng thái có mặt theo ngày",
            "005 TaskStatisticsApi.attendanceInRange",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status().name(),
            DayCareAttendanceMetric::row);

    private final ObjectProvider<TaskStatisticsApi> tasks;
    private final ObjectProvider<ResidentLocationApi> locations;

    public DayCareAttendanceMetric(
            ObjectProvider<TaskStatisticsApi> tasks, ObjectProvider<ResidentLocationApi> locations) {
        this.tasks = tasks;
        this.locations = locations;
    }

    @Override
    public Metric<AttendanceRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AttendanceRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        TaskStatisticsApi api = tasks.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        LocalDate date = LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
        ResidentLocationApi location = locations.getIfAvailable();
        return api.attendanceInRange(rough, date, date)
                .map(v -> new AttendanceRecord(
                        v,
                        location == null
                                ? null
                                : location.floorOn(v.residentId(), date).orElse(null)));
    }

    private static DetailRow row(AttendanceRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", r.view().date());
        result.put("arrivedAt", r.view().arrivedAt().orElse(null));
        result.put("leftAt", r.view().leftAt().orElse(null));
        result.put("adHocSession", r.view().adHocSession());
        return DetailRow.simple(
                r.sourceKey(),
                "005",
                "DAY_CARE_ATTENDANCE",
                r.view().status().name(),
                r.referenceTime(),
                r.floorAreaId(),
                r.residentId(),
                result);
    }
}
