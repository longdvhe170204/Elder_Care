package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ResidenceTypeAtDateView;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.dto.AttendanceView;
import com.eldercare.backend.care.api.dto.CareResultView;
import com.eldercare.backend.care.api.dto.ResultType;
import com.eldercare.backend.care.api.dto.TaskReportView;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.resident.service.DayCareAttendanceMetric;
import com.eldercare.backend.reporting.internal.resident.service.InResidenceMetric;
import com.eldercare.backend.reporting.internal.resident.service.ResidentRecords;
import com.eldercare.backend.reporting.internal.resident.service.ResidentsAtDateMetric;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.support.fake.admission.FakeResidencyStatistics;
import com.eldercare.backend.support.fake.resident.FakeResidentStatistics;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu người cao tuổi (016 T013, T014 phần {@code NCT}; tên logic {@code ChiTieu_NCT_*Test}; FR-020, FR-022, FR-023)
 * đọc {@code ResidentStatisticsApi} của 001 (trạng thái, mức hiệu lực tại ngày; lệnh trạng thái) và
 * {@code ResidencyStatisticsApi.residenceTypeAtDate} của 004 (loại lưu trú theo hợp đồng tại ngày, Q-20, Q-22).
 */
class MetricResidentsTest {

    private static final Instant FROM = Instant.parse("2026-10-31T17:00:00Z"); // 01/11 giờ Việt Nam
    private static final Instant TO = FROM.plus(Duration.ofDays(1));
    private static final LocalDate DAY = LocalDate.parse("2026-11-01");

    private final FakeResidentStatistics residents = new FakeResidentStatistics();
    private final FakeResidencyStatistics residency = new FakeResidencyStatistics()
            .atDate(
                    DAY,
                    new ResidenceTypeAtDateView(1, ResidenceType.LONG_TERM_RESIDENTIAL, ContractStatus.ACTIVE),
                    new ResidenceTypeAtDateView(2, ResidenceType.DAY_CARE, ContractStatus.ACTIVE),
                    new ResidenceTypeAtDateView(3, ResidenceType.LONG_TERM_RESIDENTIAL, ContractStatus.ACTIVE));

    private final ResidentRecords records = new ResidentRecords(
            new StaticListableBeanFactory(Map.of("r", residents)).getBeanProvider(ResidentStatisticsApi.class),
            new StaticListableBeanFactory(Map.of("a", residency)).getBeanProvider(ResidencyStatisticsApi.class),
            new StaticListableBeanFactory().getBeanProvider(ResidentLocationApi.class));

    private static <R extends MetricRecord> List<R> included(MetricSource<R> source, ReportFilters filters) {
        return source.metric().included(source.records(ScopeFilter.FACILITY, FROM, TO, filters), r -> true);
    }

    private static <R extends MetricRecord> MetricValue value(MetricSource<R> source, ReportFilters filters) {
        return source.metric().compute(included(source, filters));
    }

    private static List<Tuple> cells(MetricValue v) {
        return v.breakdown().stream()
                .map(b -> tuple(b.key(), b.value().stripTrailingZeros().toPlainString()))
                .toList();
    }

    private void atDay() {
        residents
                .statusOn(DAY, 1, ResidentStatus.IN_RESIDENCE, CareLevel.BASIC)
                .statusOn(DAY, 2, ResidentStatus.TEMPORARILY_ABSENT, CareLevel.REGULAR)
                .statusOn(DAY, 3, ResidentStatus.DECEASED, CareLevel.SPECIAL)
                .statusOn(DAY, 4, ResidentStatus.ADMISSION_IN_PROGRESS, null);
    }

    @Test
    void FR_020_inResidence_includesTemporaryAbsence_notFinalOrAdmitting() {
        atDay();

        InResidenceMetric metric = new InResidenceMetric(records);
        MetricValue v = value(metric, ReportFilters.NONE);

        assertThat(metric.metric().code()).isEqualTo("RES.IN_RESIDENCE");
        assertThat(metric.metric().group()).isEqualTo(MetricGroup.RESIDENTS);
        assertThat(v.value()).isEqualByComparingTo("2");
        assertThat(cells(v)).containsExactly(tuple("IN_RESIDENCE", "1"), tuple("TEMPORARILY_ABSENT", "1"));
    }

    private static AttendanceView attendance(long id, long resident, AttendanceStatus status) {
        return new AttendanceView(
                id,
                resident,
                DAY,
                status,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false);
    }

    /** T014 (FR-020): người bán trú có lịch hôm nay chia theo trạng thái có mặt theo ngày (005, 3.4). */
    @Test
    void FR_020_dayCareWithScheduleToday_byAttendanceStatus() {
        TaskStatisticsApi tasks = new TaskStatisticsApi() {
            @Override
            public Stream<TaskReportView> inRange(ScopeFilter scope, Instant from, Instant to) {
                return Stream.empty();
            }

            @Override
            public Stream<AttendanceView> attendanceInRange(ScopeFilter scope, LocalDate from, LocalDate to) {
                return from.equals(DAY) && to.equals(DAY)
                        ? Stream.of(
                                attendance(11, 2, AttendanceStatus.PRESENT),
                                attendance(12, 5, AttendanceStatus.REPORTED_ABSENT),
                                attendance(13, 6, AttendanceStatus.PRESENT))
                        : Stream.empty();
            }

            @Override
            public Stream<CareResultView> resultsInRange(
                    long residentId, Set<ResultType> types, Instant from, Instant to) {
                return Stream.empty();
            }

            @Override
            public Set<Long> resultRecorders(long residentId, Instant from, Instant to) {
                return Set.of();
            }
        };
        DayCareAttendanceMetric metric = new DayCareAttendanceMetric(
                new StaticListableBeanFactory(Map.of("t", tasks)).getBeanProvider(TaskStatisticsApi.class),
                new StaticListableBeanFactory().getBeanProvider(ResidentLocationApi.class));
        MetricValue v = value(metric, ReportFilters.NONE);

        assertThat(metric.metric().code()).isEqualTo("RES.DAY_CARE_ATTENDANCE");
        assertThat(metric.metric().group()).isEqualTo(MetricGroup.RESIDENTS);
        assertThat(v.value()).isEqualByComparingTo("3");
        assertThat(cells(v)).containsExactly(tuple("PRESENT", "2"), tuple("REPORTED_ABSENT", "1"));
        assertThat(metric.metric().details(included(metric, ReportFilters.NONE), "PRESENT"))
                .extracting(r -> r.residentId())
                .containsExactly(2L, 6L);
    }

    /** FR-060 (chủ dự án duyệt 2026-11-11): lọc mức chăm sóc theo mã enum của 001, mức hiệu lực tại ngày. */
    @Test
    void FR_060_careLevelFilter_atDate() {
        atDay();
        ResidentsAtDateMetric metric = new ResidentsAtDateMetric(records);

        ReportFilters regular = new ReportFilters(null, null, "REGULAR", null, null, null, null);

        assertThat(value(metric, regular).value()).isEqualByComparingTo("1");
        assertThat(value(new InResidenceMetric(records), regular).value()).isEqualByComparingTo("1");
    }
}
