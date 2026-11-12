package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.TaskReportView;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.staffing.service.StaffingRatioMetric;
import com.eldercare.backend.reporting.internal.staffing.service.StaffingRatioMetric.StaffingRatioRecord;
import com.eldercare.backend.reporting.internal.task.service.TaskRecords;
import com.eldercare.backend.reporting.internal.task.service.TodayTasksMetric;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.StaffingRatioView;
import com.eldercare.backend.support.fake.staffing.FakeStaffStatistics;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * FR-006: lúc 01:00 giờ Việt Nam, "hôm nay" là ngày dương lịch theo thời điểm dự kiến (việc 23:30 hôm qua của ca đêm
 * không vào hôm nay); "ca hiện tại" là ca đang diễn ra, kể cả ca bắt đầu từ hôm qua.
 */
class FR_006_TodayAndCurrentShiftTest {
    /** 2026-11-15 01:00 giờ Việt Nam. */
    private static final Instant NOW = Instant.parse("2026-11-14T18:00:00Z");

    private final BusinessTime time = new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE));

    private static TaskReportView task(long id, Instant planned) {
        return new TaskReportView(
                id,
                Optional.of(7L),
                3,
                101,
                9,
                TaskStatus.DUE,
                List.of(),
                Importance.NORMAL,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                planned,
                Optional.empty(),
                Optional.empty(),
                false,
                "NONE",
                List.of());
    }

    private static StaffingRatioView ratio(long shiftId, Instant start, Instant end) {
        return new StaffingRatioView(
                shiftId * 10,
                shiftId,
                101L,
                start.plusSeconds(60),
                new BigDecimal("7.00"),
                new BigDecimal("8.00"),
                true,
                false,
                5,
                new BigDecimal("35.00"),
                3L,
                start,
                end);
    }

    @Test
    void FR_006_todayIsCalendarDayOfPlannedTime_currentShiftMayStartYesterday() {
        Instant from = time.startOfDay(time.today());
        Instant to = time.startOfDay(time.today().plusDays(1));
        assertThat(from).isEqualTo(Instant.parse("2026-11-14T17:00:00Z"));

        var tasks = new MetricCareTasksTest.Tasks();
        tasks.rows.add(task(1, from.minusSeconds(1800)));
        tasks.rows.add(task(2, from.plusSeconds(1800)));
        var today = new TodayTasksMetric(new TaskRecords(
                new StaticListableBeanFactory(Map.of("tasks", tasks)).getBeanProvider(TaskStatisticsApi.class)));
        assertThat(today.metric()
                        .included(today.records(ScopeFilter.FACILITY, from, to, ReportFilters.NONE), r -> true))
                .extracting(r -> r.view().id())
                .containsExactly(2L);

        var statistics = new FakeStaffStatistics();
        Instant night = from.minusSeconds(2 * 3600);
        statistics.ratios(
                List.of(ratio(1, night.minusSeconds(8 * 3600), night), ratio(2, night, night.plusSeconds(8 * 3600))));
        var ratios = new StaffingRatioMetric(
                new StaticListableBeanFactory(Map.of("statistics", statistics))
                        .getBeanProvider(StaffStatisticsApi.class),
                time);
        assertThat(ratios.current(ScopeFilter.FACILITY, from, NOW))
                .extracting(StaffingRatioRecord::shiftId)
                .containsExactly(2L);
    }
}
