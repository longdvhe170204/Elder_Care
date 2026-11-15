package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.AttendanceView;
import com.eldercare.backend.care.api.dto.CareResultView;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.ResultType;
import com.eldercare.backend.care.api.dto.TaskReportView;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.task.service.LateRecordingsMetric;
import com.eldercare.backend.reporting.internal.task.service.MandatoryOnTimeRateMetric;
import com.eldercare.backend.reporting.internal.task.service.OverdueTasksMetric;
import com.eldercare.backend.reporting.internal.task.service.TaskRecords;
import com.eldercare.backend.reporting.internal.task.service.TasksByStaffMetric;
import com.eldercare.backend.reporting.internal.task.service.TodayTasksMetric;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu công việc (016 T013 → T016 phần công việc; tên logic {@code ChiTieu_CV_*Test}; FR-020, FR-030 → FR-032)
 * đọc {@code TaskStatisticsApi.inRange} của 005: trạng thái 8.3, người đóng, quy tắc đóng, nhãn ghi nhận muộn và các mốc
 * Quá hạn do 005 xác định (FR-002).
 */
class MetricCareTasksTest {

    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-02T00:00:00Z");

    /** Nguồn 005: trả công việc có thời điểm dự kiến trong khoảng. */
    static final class Tasks implements TaskStatisticsApi {
        final List<TaskReportView> rows = new ArrayList<>();

        @Override
        public Stream<TaskReportView> inRange(ScopeFilter scope, Instant from, Instant to) {
            return rows.stream()
                    .filter(r -> !r.plannedAt().isBefore(from) && r.plannedAt().isBefore(to));
        }

        @Override
        public Stream<AttendanceView> attendanceInRange(ScopeFilter scope, LocalDate from, LocalDate to) {
            return Stream.empty();
        }

        @Override
        public Stream<CareResultView> resultsInRange(long residentId, Set<ResultType> types, Instant from, Instant to) {
            return Stream.empty();
        }

        @Override
        public Set<Long> resultRecorders(long residentId, Instant from, Instant to) {
            return Set.of();
        }
    }

    private final Tasks tasks = new Tasks();
    private final TaskRecords records = new TaskRecords(
            new StaticListableBeanFactory(Map.of("tasks", tasks)).getBeanProvider(TaskStatisticsApi.class));

    private static Instant at(int minutes) {
        return FROM.plus(Duration.ofMinutes(minutes));
    }

    private static TaskReportView task(
            long id,
            Long resident,
            TaskStatus status,
            Importance importance,
            Long performer,
            Long closedBy,
            String rule,
            int plannedMin,
            boolean late,
            List<Instant> overdueAt) {
        return new TaskReportView(
                id,
                Optional.ofNullable(resident),
                3,
                101,
                9,
                status,
                List.of(),
                importance,
                Optional.ofNullable(performer),
                Optional.ofNullable(closedBy),
                Optional.ofNullable(rule),
                at(plannedMin),
                Optional.empty(),
                Optional.empty(),
                late,
                "NONE",
                overdueAt);
    }

    private static <R extends MetricRecord> List<R> included(MetricSource<R> source) {
        return source.metric().included(source.records(ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE), r -> true);
    }

    private static <R extends MetricRecord> MetricValue value(MetricSource<R> source) {
        return source.metric().compute(included(source));
    }

    private static List<Tuple> cells(MetricValue v) {
        return v.breakdown().stream()
                .map(b -> tuple(b.key(), b.value().stripTrailingZeros().toPlainString()))
                .toList();
    }

    private void sample() {
        tasks.rows.addAll(List.of(
                task(1, 7L, TaskStatus.COMPLETED, Importance.MANDATORY, 51L, 51L, null, 60, false, List.of(at(90))),
                task(
                        2,
                        7L,
                        TaskStatus.COMPLETED_LATE,
                        Importance.MANDATORY,
                        51L,
                        51L,
                        null,
                        120,
                        true,
                        List.of(at(150))),
                task(3, 8L, TaskStatus.NOT_PERFORMED, Importance.MANDATORY, 52L, 52L, null, 180, false, List.of()),
                task(
                        4,
                        8L,
                        TaskStatus.NOT_PERFORMED,
                        Importance.MANDATORY,
                        null,
                        null,
                        "AFTER_HANDOVER",
                        240,
                        false,
                        List.of()),
                task(
                        5,
                        8L,
                        TaskStatus.CANCELLED,
                        Importance.MANDATORY,
                        null,
                        null,
                        "FINAL_STATUS",
                        300,
                        false,
                        List.of()),
                task(6, null, TaskStatus.OVERDUE, Importance.NORMAL, null, null, null, 360, false, List.of(at(400))),
                task(7, 7L, TaskStatus.DUE, Importance.IMPORTANT, 51L, null, null, 420, true, List.of()),
                task(8, 7L, TaskStatus.COMPLETED, Importance.NORMAL, 51L, 51L, null, 24 * 60, false, List.of())));
    }

    @Test
    void FR_020_today_byStatus_overdueNow_byImportance() {
        sample();

        TodayTasksMetric today = new TodayTasksMetric(records);
        MetricValue t = value(today);
        assertThat(today.metric().code()).isEqualTo("TASK.TODAY");
        assertThat(today.metric().group()).isEqualTo(MetricGroup.CARE_TASKS);
        assertThat(t.value()).isEqualByComparingTo("7");

        OverdueTasksMetric overdue = new OverdueTasksMetric(records);
        assertThat(overdue.metric().code()).isEqualTo("TASK.OVERDUE");
        // báo cáo: công việc đã từng Quá hạn trong khoảng, kể cả sau đó Hoàn thành (FR-030)
        assertThat(cells(value(overdue))).containsExactly(tuple("MANDATORY", "2"), tuple("NORMAL", "1"));
        // dashboard: đang Quá hạn lúc xem
        List<?> now = overdue.metric()
                .included(overdue.current(ScopeFilter.FACILITY, FROM, TO).map(r -> r), r -> true);
        assertThat(now).hasSize(1);
    }

    @Test
    void FR_031_mandatoryOnTimeRate_excludesSystemClosedAndCancelledAndOpen() {
        sample();

        MandatoryOnTimeRateMetric rate = new MandatoryOnTimeRateMetric(records);
        MetricValue v = value(rate);

        assertThat(rate.metric().code()).isEqualTo("TASK.MANDATORY_ON_TIME_RATE");
        // Hoàn thành ÷ (Hoàn thành + Hoàn thành trễ + Không thực hiện do người đóng); việc 1 từng Quá hạn vẫn đúng hạn
        assertThat(v.numerator()).isEqualByComparingTo("1");
        assertThat(v.denominator()).isEqualByComparingTo("3");
        assertThat(v.value()).isEqualByComparingTo("33.3");
    }

    @Test
    void FR_032_lateRecordings_FR_030_byStaff_commonTasksOwnRow() {
        sample();

        LateRecordingsMetric late = new LateRecordingsMetric(records);
        assertThat(late.metric().code()).isEqualTo("TASK.LATE_RECORDINGS");
        assertThat(cells(value(late))).containsExactly(tuple("51", "2"));

        TasksByStaffMetric byStaff = new TasksByStaffMetric(records);
        assertThat(byStaff.metric().code()).isEqualTo("TASK.BY_STAFF");
        assertThat(byStaff.metric().group()).isEqualTo(MetricGroup.PER_STAFF);
        assertThat(cells(value(byStaff)))
                .containsExactly(tuple("51", "3"), tuple("52", "1"), tuple("COMMON", "1"), tuple("UNASSIGNED", "2"));
        assertThat(byStaff.metric().details(included(byStaff), "COMMON"))
                .extracting(r -> r.sourceKey(), r -> r.residentId())
                .containsExactly(tuple("M04.TASK:6", null));
    }
}
