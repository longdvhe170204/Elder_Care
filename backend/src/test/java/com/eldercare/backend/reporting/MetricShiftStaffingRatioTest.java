package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.staffing.service.StaffingRatioMetric;
import com.eldercare.backend.reporting.internal.staffing.service.StaffingRatioMetric.StaffingRatioRecord;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.StaffingRatioView;
import com.eldercare.backend.support.fake.staffing.FakeStaffStatistics;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu {@code SHIFT.STAFFING_RATIO} (tên logic {@code CA.TY_LE_PHUC_VU}; 016 T023, FR-020, FR-070, US1 #4, Q-79, Q-200):
 * dashboard là ca đang diễn ra, giá trị và tình trạng do 008 tính; báo cáo là ca đã diễn ra có giờ bắt đầu trong khoảng,
 * theo lần tính cuối trước khi ca kết thúc, "số ca không đạt" đếm theo dấu "từng không đạt"; ngưỡng chưa cấu hình không
 * hiện đạt hay không đạt.
 */
class MetricShiftStaffingRatioTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    private final FakeStaffStatistics statistics = new FakeStaffStatistics();

    private final StaffingRatioMetric source = new StaffingRatioMetric(
            new StaticListableBeanFactory(Map.of("statistics", statistics)).getBeanProvider(StaffStatisticsApi.class),
            new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));

    private final BusinessTime time = new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE));

    private static StaffingRatioView ratio(
            long shiftId, long floor, Instant start, Instant end, String threshold, boolean passed, boolean ever) {
        return new StaffingRatioView(
                shiftId * 10,
                shiftId,
                floor,
                start.plusSeconds(60),
                new BigDecimal("7.00"),
                threshold == null ? null : new BigDecimal(threshold),
                passed,
                ever,
                5,
                new BigDecimal("35.00"),
                3L,
                start,
                end);
    }

    @Test
    void FR_020_dashboard_currentShiftOnly_withStatusByArea() {
        Instant start = NOW.minus(Duration.ofHours(3));
        statistics.ratios(List.of(
                ratio(1, 101, start, start.plus(Duration.ofHours(8)), "8.00", false, true),
                ratio(2, 102, start, start.plus(Duration.ofHours(8)), null, false, false),
                ratio(3, 101, start.minus(Duration.ofHours(8)), start, "8.00", true, false)));
        Metric<StaffingRatioRecord> metric = source.metric();

        List<StaffingRatioRecord> included = metric.included(
                source.current(ScopeFilter.FACILITY, time.startOfDay(time.today()), NOW), ReportScope.FACILITY::covers);
        MetricValue value = metric.compute(included);
        List<DetailRow> rows = metric.details(included, null);

        assertThat(metric.code()).isEqualTo("SHIFT.STAFFING_RATIO");
        assertThat(metric.group()).isEqualTo(MetricGroup.STAFFING_RATIO);
        assertThat(value.value()).isEqualByComparingTo("2");
        assertThat(value.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("NOT_CONFIGURED", 1),
                        org.assertj.core.groups.Tuple.tuple("NOT_MET", 1));
        assertThat(rows).extracting(DetailRow::floorAreaId).containsExactly(101L, 102L);
        assertThat(rows.getFirst().result())
                .containsEntry("ratio", new BigDecimal("7.00"))
                .containsEntry("staffCount", 5)
                .containsEntry("totalWeight", new BigDecimal("35.00"))
                .containsEntry("passed", false);
        assertThat(rows.get(1).result()).containsEntry("threshold", null);
    }

    @Test
    void FR_070_report_endedShiftsStartingInPeriod_notMetCountedByEverBelowFlag() {
        LocalDate first = LocalDate.of(2026, 11, 1);
        Instant from = time.startOfDay(first);
        Instant to = time.startOfDay(first.plusMonths(1));
        Instant nov2 = time.startOfDay(first.plusDays(1));
        statistics.ratios(List.of(
                ratio(4, 101, nov2, nov2.plus(Duration.ofHours(8)), "8.00", true, true),
                ratio(5, 101, nov2.plus(Duration.ofDays(1)), nov2.plus(Duration.ofHours(32)), "8.00", true, false),
                ratio(6, 101, from.minus(Duration.ofHours(1)), from.plus(Duration.ofHours(7)), "8.00", false, true),
                ratio(7, 101, NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(7)), "8.00", false, true)));
        Metric<StaffingRatioRecord> metric = source.metric();

        List<StaffingRatioRecord> included = metric.included(
                source.records(ScopeFilter.FACILITY, from, to, ReportFilters.NONE), ReportScope.FACILITY::covers);
        MetricValue value = metric.compute(included);

        assertThat(value.value()).isEqualByComparingTo("2");
        assertThat(value.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("MET", 1),
                        org.assertj.core.groups.Tuple.tuple("NOT_MET", 1));
        assertThat(included)
                .extracting(StaffingRatioRecord::referenceTime)
                .containsExactly(nov2, nov2.plus(Duration.ofDays(1)));
        assertThat(metric.details(included, "NOT_MET"))
                .singleElement()
                .satisfies(r -> assertThat(r.result())
                        .containsEntry("passed", true)
                        .containsEntry("everBelowThreshold", true)
                        .containsEntry("shiftTemplateId", 3L));
    }

    @Test
    void US8_2_floorManagerScope_onlyOwnFloor() {
        Instant start = NOW.minus(Duration.ofHours(3));
        statistics.ratios(List.of(
                ratio(8, 101, start, start.plus(Duration.ofHours(8)), "8.00", true, false),
                ratio(9, 102, start, start.plus(Duration.ofHours(8)), "8.00", true, false)));
        ScopeFilter floor = new ScopeFilter(false, Set.of(101L), Set.of(), Set.of());
        ReportScope scope = ReportScope.of(List.of(ReportScope.Grant.always(floor)));
        Metric<StaffingRatioRecord> metric = source.metric();

        List<StaffingRatioRecord> included =
                metric.included(source.current(floor, time.startOfDay(time.today()), NOW), scope::covers);

        assertThat(included).extracting(StaffingRatioRecord::shiftId).containsExactly(8L);
    }
}
