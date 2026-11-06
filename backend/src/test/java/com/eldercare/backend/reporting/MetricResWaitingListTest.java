package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ResidenceTypeAtDateView;
import com.eldercare.backend.admission.api.dto.WaitingEntryView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.waitinglist.service.WaitingListMetric;
import com.eldercare.backend.reporting.internal.waitinglist.service.WaitingListRecords;
import com.eldercare.backend.reporting.internal.waitinglist.service.WaitingTimeMaximumMetric;
import com.eldercare.backend.reporting.internal.waitinglist.service.WaitingTimeMetric;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu danh sách chờ (tên logic {@code NCT.DANH_SACH_CHO}; 016 T106, FR-025) đọc {@code ResidencyStatisticsApi} của
 * 004: số hồ sơ theo trạng thái hồ sơ chờ có ngày đăng ký trong khoảng, lọc loại lưu trú; thời gian chờ trung bình, dài
 * nhất (ngày) của hồ sơ đang chờ lúc xem.
 */
class MetricResWaitingListTest {

    /** 10:00 ngày 15/11/2026 giờ Việt Nam. */
    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    static final class StubResidencyStatistics implements ResidencyStatisticsApi {
        final List<WaitingEntryView> entries = new ArrayList<>();
        final List<LocalDate[]> asked = new ArrayList<>();

        @Override
        public Stream<ResidenceTypeAtDateView> residenceTypeAtDate(ScopeFilter scope, LocalDate date) {
            return Stream.empty();
        }

        @Override
        public Stream<WaitingEntryView> waitingList(LocalDate from, LocalDate to) {
            asked.add(new LocalDate[] {from, to});
            return entries.stream()
                    .filter(e -> !e.applicationDate().isBefore(from)
                            && !e.applicationDate().isAfter(to));
        }
    }

    private final StubResidencyStatistics statistics = new StubResidencyStatistics();
    private final WaitingListRecords records = new WaitingListRecords(
            new StaticListableBeanFactory(Map.of("statistics", statistics))
                    .getBeanProvider(ResidencyStatisticsApi.class),
            new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));
    private final WaitingListMetric list = new WaitingListMetric(records);
    private final WaitingTimeMetric average = new WaitingTimeMetric(records);
    private final WaitingTimeMaximumMetric maximum = new WaitingTimeMaximumMetric(records);

    /** Đầu ngày 01/11 và đầu ngày 16/11 giờ Việt Nam: khoảng là 01/11 → 15/11. */
    private static final Instant FROM = Instant.parse("2026-10-31T17:00:00Z");

    private static final Instant TO = Instant.parse("2026-11-15T17:00:00Z");

    private void add(long id, String status, ResidenceType type, String applicationDate) {
        statistics.entries.add(
                new WaitingEntryView(id, status, type, CareLevel.BASIC, LocalDate.parse(applicationDate), null));
    }

    private <R extends MetricRecord> MetricValue value(MetricSource<R> source, ReportFilters filters) {
        Metric<R> metric = source.metric();
        return metric.compute(metric.included(source.records(ScopeFilter.FACILITY, FROM, TO, filters), r -> true));
    }

    @Test
    void FR_025_countByStatus_applicationDateInPeriod_filteredByResidenceType() {
        add(1, "WAITING", ResidenceType.LONG_TERM_RESIDENTIAL, "2026-11-01");
        add(2, "CONTACTED", ResidenceType.DAY_CARE, "2026-11-05");
        add(3, "ADMITTED", ResidenceType.LONG_TERM_RESIDENTIAL, "2026-11-15");
        add(4, "WAITING", ResidenceType.LONG_TERM_RESIDENTIAL, "2026-10-31");

        MetricValue all = value(list, ReportFilters.NONE);
        MetricValue dayCare = value(list, new ReportFilters(Set.of(), "DAY_CARE", null, null, null, null, null));

        assertThat(list.metric().code()).isEqualTo("RES.WAITING_LIST");
        assertThat(list.metric().group()).isEqualTo(MetricGroup.WAITING_LIST);
        assertThat(statistics.asked.getFirst())
                .containsExactly(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-15"));
        assertThat(all.value()).isEqualByComparingTo("3");
        assertThat(all.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(Tuple.tuple("ADMITTED", 1), Tuple.tuple("CONTACTED", 1), Tuple.tuple("WAITING", 1));
        assertThat(dayCare.value()).isEqualByComparingTo("1");
        assertThat(list.metric()
                        .details(
                                list.metric()
                                        .included(
                                                list.records(ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE),
                                                r -> true),
                                "CONTACTED"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.feature()).isEqualTo("004");
                    assertThat(row.result())
                            .containsEntry("residenceType", "DAY_CARE")
                            .containsEntry("careLevel", "BASIC");
                });
    }

    @Test
    void FR_025_waitingTime_ofOpenEntriesAtViewTime_inDays() {
        add(1, "WAITING", ResidenceType.LONG_TERM_RESIDENTIAL, "2026-11-01");
        add(2, "CONTACTED", ResidenceType.DAY_CARE, "2026-11-05");
        add(3, "ADMITTED", ResidenceType.LONG_TERM_RESIDENTIAL, "2026-10-01");
        add(4, "WAITING_CANCELLED", ResidenceType.LONG_TERM_RESIDENTIAL, "2026-09-01");

        assertThat(average.metric().code()).isEqualTo("RES.WAITING_TIME");
        assertThat(maximum.metric().code()).isEqualTo("RES.WAITING_TIME_MAX");
        assertThat(value(average, ReportFilters.NONE).value()).isEqualByComparingTo("12.0");
        assertThat(value(maximum, ReportFilters.NONE).value()).isEqualByComparingTo("14");
    }

    @Test
    void FR_025_noSourceBean_isEmpty() {
        WaitingListRecords none = new WaitingListRecords(
                new StaticListableBeanFactory().getBeanProvider(ResidencyStatisticsApi.class),
                new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));

        assertThat(new WaitingListMetric(none).records(ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE))
                .isEmpty();
    }
}
