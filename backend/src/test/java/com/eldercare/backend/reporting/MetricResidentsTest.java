package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ResidenceTypeAtDateView;
import com.eldercare.backend.admission.api.dto.WaitingEntryView;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.resident.service.InResidenceMetric;
import com.eldercare.backend.reporting.internal.resident.service.MovementsMetric;
import com.eldercare.backend.reporting.internal.resident.service.ResidentRecords;
import com.eldercare.backend.reporting.internal.resident.service.ResidentsAtDateMetric;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentChangeView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.support.fake.resident.FakeResidentStatistics;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ResidencyStatisticsApi residency = new ResidencyStatisticsApi() {
        @Override
        public Stream<ResidenceTypeAtDateView> residenceTypeAtDate(ScopeFilter scope, LocalDate date) {
            return date.equals(DAY)
                    ? Stream.of(
                            new ResidenceTypeAtDateView(1, ResidenceType.LONG_TERM_RESIDENTIAL, ContractStatus.ACTIVE),
                            new ResidenceTypeAtDateView(2, ResidenceType.DAY_CARE, ContractStatus.ACTIVE),
                            new ResidenceTypeAtDateView(3, ResidenceType.LONG_TERM_RESIDENTIAL, ContractStatus.ACTIVE))
                    : Stream.empty();
        }

        @Override
        public Stream<WaitingEntryView> waitingList(LocalDate from, LocalDate to) {
            return Stream.empty();
        }
    };

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

    @Test
    void FR_022_atDate_byResidenceType_undeterminedWithoutContract_filterByType() {
        atDay();

        ResidentsAtDateMetric metric = new ResidentsAtDateMetric(records);
        MetricValue v = value(metric, ReportFilters.NONE);

        assertThat(metric.metric().code()).isEqualTo("RES.AT_DATE");
        // người đã ở trạng thái cuối trước ngày vẫn đếm nếu 001 trả trạng thái tại ngày đó
        assertThat(v.value()).isEqualByComparingTo("4");
        assertThat(cells(v))
                .containsExactly(
                        tuple("DAY_CARE", "1"), tuple("LONG_TERM_RESIDENTIAL", "2"), tuple("UNDETERMINED", "1"));
        assertThat(metric.metric().details(included(metric, ReportFilters.NONE), "DAY_CARE"))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.residentId()).isEqualTo(2L);
                    assertThat(r.status()).isEqualTo("TEMPORARILY_ABSENT");
                    assertThat(r.result()).containsEntry("careLevel", "REGULAR");
                });

        ReportFilters dayCare = new ReportFilters(null, "DAY_CARE", null, null, null, null, null);
        assertThat(value(metric, dayCare).value()).isEqualByComparingTo("1");
    }

    @Test
    void FR_023_movements_byCommandAtEffectiveTime_returnSplitByOrigin_careLevelChange() {
        Instant at = FROM.plus(Duration.ofHours(8));
        residents
                .change(change(
                        1,
                        at,
                        StatusCommand.COMPLETE_ADMISSION,
                        ResidentStatus.ADMISSION_IN_PROGRESS,
                        ResidentStatus.IN_RESIDENCE,
                        null))
                .change(change(
                        2,
                        at,
                        StatusCommand.GRANT_TEMPORARY_ABSENCE,
                        ResidentStatus.IN_RESIDENCE,
                        ResidentStatus.TEMPORARILY_ABSENT,
                        null))
                .change(change(
                        2,
                        at.plusSeconds(60),
                        StatusCommand.RECORD_RETURN,
                        ResidentStatus.TEMPORARILY_ABSENT,
                        ResidentStatus.IN_RESIDENCE,
                        null))
                .change(change(
                        3,
                        at,
                        StatusCommand.TRANSFER_OUT,
                        ResidentStatus.IN_RESIDENCE,
                        ResidentStatus.HOSPITALIZED,
                        null))
                .change(change(
                        3,
                        at.plusSeconds(60),
                        StatusCommand.RECORD_RETURN,
                        ResidentStatus.HOSPITALIZED,
                        ResidentStatus.IN_RESIDENCE,
                        null))
                .change(change(
                        4,
                        at,
                        StatusCommand.ROLL_CALL_DEPARTURE,
                        ResidentStatus.IN_RESIDENCE,
                        ResidentStatus.OFF_SITE_ACTIVITY,
                        null))
                .change(new ResidentChangeView(
                        5, at, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(CareLevel.SPECIAL)))
                .change(change(
                        6, TO, StatusCommand.RECORD_DEATH, ResidentStatus.IN_RESIDENCE, ResidentStatus.DECEASED, null));

        MovementsMetric metric = new MovementsMetric(records);

        assertThat(metric.metric().code()).isEqualTo("RES.MOVEMENTS");
        assertThat(cells(value(metric, ReportFilters.NONE)))
                .containsExactly(
                        tuple("CARE_LEVEL_CHANGE", "1"),
                        tuple("COMPLETE_ADMISSION", "1"),
                        tuple("GRANT_TEMPORARY_ABSENCE", "1"),
                        tuple("RECORD_RETURN:HOSPITALIZED", "1"),
                        tuple("RECORD_RETURN:TEMPORARILY_ABSENT", "1"),
                        tuple("TRANSFER_OUT", "1"));
    }

    private static ResidentChangeView change(
            long resident, Instant at, StatusCommand command, ResidentStatus from, ResidentStatus to, CareLevel level) {
        return new ResidentChangeView(
                resident, at, Optional.of(command), Optional.of(from), Optional.of(to), Optional.ofNullable(level));
    }
}
