package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.care.api.ActivityStatisticsApi;
import com.eldercare.backend.care.api.dto.IsolationRiskView;
import com.eldercare.backend.care.api.dto.QualityCheckStatisticsView;
import com.eldercare.backend.care.api.dto.SessionStatisticsView;
import com.eldercare.backend.care.api.dto.TripStatisticsView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.NutritionStatisticsApi;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.dto.PortionReportView;
import com.eldercare.backend.reporting.internal.activity.service.ActivityRecords;
import com.eldercare.backend.reporting.internal.activity.service.MissingParticipantsMetric;
import com.eldercare.backend.reporting.internal.activity.service.OverdueTripsMetric;
import com.eldercare.backend.reporting.internal.activity.service.ParticipationRateMetric;
import com.eldercare.backend.reporting.internal.activity.service.SessionsMetric;
import com.eldercare.backend.reporting.internal.meal.service.DiscrepancyTicketsMetric;
import com.eldercare.backend.reporting.internal.meal.service.LateTicketsMetric;
import com.eldercare.backend.reporting.internal.meal.service.MealSlipRecords;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.quality.service.ChecksByRoleMetric;
import com.eldercare.backend.reporting.internal.quality.service.PassRateMetric;
import com.eldercare.backend.reporting.internal.quality.service.QualityCheckRecords;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chỉ tiêu phiếu bữa ăn, hoạt động, kiểm tra chất lượng (016 T015, T016 phần {@code BUA}, {@code HD}, {@code KT}; FR-020,
 * FR-034, FR-035, FR-037) đọc {@code NutritionStatisticsApi.slips} của 011 và {@code ActivityStatisticsApi} của 014: dấu
 * giao trễ, sai lệch, kết quả điểm danh, kết quả kiểm tra do nguồn xác định (FR-002).
 */
class MetricMealActivityQualityTest {

    private static final Instant FROM = Instant.parse("2026-10-31T17:00:00Z"); // 01/11 00:00 giờ Việt Nam
    private static final Instant TO = FROM.plus(Duration.ofDays(1));
    private static final LocalDate DAY = LocalDate.parse("2026-11-01");
    private static final Instant NOW = FROM.plus(Duration.ofHours(20));

    private final List<MealSlipReportView> slips = new ArrayList<>();
    private final List<SessionStatisticsView> sessions = new ArrayList<>();
    private final List<TripStatisticsView> trips = new ArrayList<>();
    private final List<QualityCheckStatisticsView> checks = new ArrayList<>();

    private final NutritionStatisticsApi nutrition = new NutritionStatisticsApi() {
        @Override
        public Stream<MealSlipReportView> slips(ScopeFilter scope, LocalDate from, LocalDate to) {
            return slips.stream()
                    .filter(s -> !s.date().isBefore(from) && !s.date().isAfter(to));
        }

        @Override
        public Stream<PortionReportView> portions(ScopeFilter scope, LocalDate from, LocalDate to) {
            return Stream.empty();
        }

        @Override
        public Stream<FamilyFoodReportView> familyFoods(ScopeFilter scope, LocalDate from, LocalDate to) {
            return Stream.empty();
        }
    };

    private final ActivityStatisticsApi activities = new ActivityStatisticsApi() {
        @Override
        public Stream<SessionStatisticsView> sessions(ScopeFilter scope, LocalDate from, LocalDate to) {
            return sessions.stream();
        }

        @Override
        public Stream<TripStatisticsView> trips(ScopeFilter scope, LocalDate from, LocalDate to) {
            return trips.stream();
        }

        @Override
        public Stream<QualityCheckStatisticsView> qualityChecks(ScopeFilter scope, LocalDate from, LocalDate to) {
            return checks.stream();
        }

        @Override
        public Stream<IsolationRiskView> isolationRisks() {
            return Stream.empty();
        }
    };

    private final ParameterApi parameters = new ParameterApi() {
        @Override
        public ParameterValue read(ParameterCode code) {
            return new ParameterValue(
                    code,
                    ParameterKind.DURATION,
                    JsonMapper.builder().build().readTree("{\"value\":30,\"unit\":\"MINUTES\"}"));
        }

        @Override
        public ParameterValue readAt(ParameterCode code, Instant at) {
            return read(code);
        }
    };

    private final BusinessTime time = new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE));
    private final MealSlipRecords mealRecords = new MealSlipRecords(
            new StaticListableBeanFactory(Map.of("n", nutrition)).getBeanProvider(NutritionStatisticsApi.class));
    private final ActivityRecords activityRecords = new ActivityRecords(
            new StaticListableBeanFactory(Map.of("a", activities)).getBeanProvider(ActivityStatisticsApi.class),
            parameters,
            time);
    private final QualityCheckRecords qualityRecords = new QualityCheckRecords(
            new StaticListableBeanFactory(Map.of("a", activities)).getBeanProvider(ActivityStatisticsApi.class));

    private static <R extends MetricRecord> List<R> included(MetricSource<R> source) {
        return source.metric().included(source.records(ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE), r -> true);
    }

    private static <R extends MetricRecord> MetricValue value(MetricSource<R> source) {
        return source.metric().compute(included(source));
    }

    private static List<Tuple> cells(MetricValue v) {
        return v.breakdown().stream()
                .map(b -> tuple(
                        b.key(),
                        b.value() == null
                                ? null
                                : b.value().stripTrailingZeros().toPlainString()))
                .toList();
    }

    private static Instant at(int minutes) {
        return FROM.plus(Duration.ofMinutes(minutes));
    }

    @Test
    void FR_034_lateAndDiscrepancyTickets_byMeal() {
        slips.add(new MealSlipReportView(1, DAY, 11, 101, MealSlipStatus.values()[0], at(400), true, false));
        slips.add(new MealSlipReportView(2, DAY, 11, 102, MealSlipStatus.values()[0], at(380), false, true));
        slips.add(new MealSlipReportView(3, DAY, 12, 101, MealSlipStatus.values()[0], null, true, true));
        slips.add(new MealSlipReportView(4, DAY.plusDays(1), 11, 101, MealSlipStatus.values()[0], null, true, true));

        LateTicketsMetric late = new LateTicketsMetric(mealRecords);
        assertThat(late.metric().code()).isEqualTo("MEAL.LATE_TICKETS");
        assertThat(late.metric().group()).isEqualTo(MetricGroup.MEAL_TICKETS);
        assertThat(cells(value(late))).containsExactly(tuple("11", "1"), tuple("12", "1"));

        DiscrepancyTicketsMetric discrepancy = new DiscrepancyTicketsMetric(mealRecords);
        assertThat(discrepancy.metric().code()).isEqualTo("MEAL.DISCREPANCY_TICKETS");
        assertThat(value(discrepancy).value()).isEqualByComparingTo("2");
        assertThat(discrepancy.metric().details(included(discrepancy), null))
                .extracting(r -> r.sourceKey(), r -> r.floorAreaId())
                .containsExactly(tuple("M08.MEAL_SLIP:2", 102L), tuple("M08.MEAL_SLIP:3", 101L));
    }

    @Test
    void FR_035_sessions_participationRate_awayNoShowNotRecordedNotInDenominator() {
        sessions.add(new SessionStatisticsView(
                1,
                5,
                Optional.of(101L),
                at(540),
                "COMPLETED",
                false,
                10,
                4,
                Map.of("REFUSED", 1, "HEALTH", 1, "AWAY", 2, "NO_SHOW", 1),
                1));
        sessions.add(
                new SessionStatisticsView(2, 6, Optional.of(101L), at(600), "CANCELLED", false, 3, 0, Map.of(), 0));
        sessions.add(new SessionStatisticsView(
                3, 6, Optional.of(102L), at(660), "COMPLETED", false, 2, 1, Map.of("OTHER", 1), 0));

        SessionsMetric count = new SessionsMetric(activityRecords);
        assertThat(count.metric().code()).isEqualTo("ACT.SESSIONS");
        assertThat(count.metric().group()).isEqualTo(MetricGroup.ACTIVITIES);
        assertThat(cells(value(count))).containsExactly(tuple("CANCELLED", "1"), tuple("COMPLETED", "2"));

        ParticipationRateMetric rate = new ParticipationRateMetric(activityRecords);
        MetricValue v = value(rate);
        assertThat(rate.metric().code()).isEqualTo("ACT.PARTICIPATION_RATE");
        // Có mặt 5 ÷ (Có mặt 5 + từ chối 1 + sức khỏe 1 + khác 1)
        assertThat(v.numerator()).isEqualByComparingTo("5");
        assertThat(v.denominator()).isEqualByComparingTo("8");
        assertThat(cells(v)).containsExactly(tuple("5", "66.7"), tuple("6", "50"));
    }

    @Test
    void FR_035_overdueTrips_doubleThresholdSeparately_missingParticipants() {
        // giờ về dự kiến hiện hành (sau gia hạn) + 30 phút
        trips.add(new TripStatisticsView(1, Optional.of(101L), at(600), List.of(), Optional.of(at(640)), 0));
        trips.add(new TripStatisticsView(2, Optional.of(101L), at(600), List.of(at(700)), Optional.of(at(720)), 0));
        trips.add(new TripStatisticsView(3, Optional.of(102L), at(600), List.of(), Optional.of(at(670)), 2));
        trips.add(new TripStatisticsView(4, Optional.of(102L), at(600), List.of(), Optional.empty(), 0));
        trips.add(new TripStatisticsView(5, Optional.of(102L), at(1190), List.of(), Optional.empty(), 0));

        OverdueTripsMetric overdue = new OverdueTripsMetric(activityRecords);
        assertThat(overdue.metric().code()).isEqualTo("ACT.OVERDUE_TRIPS");
        assertThat(cells(value(overdue))).containsExactly(tuple("OVERDUE", "1"), tuple("OVERDUE_DOUBLE", "2"));

        MissingParticipantsMetric missing = new MissingParticipantsMetric(activityRecords);
        assertThat(missing.metric().code()).isEqualTo("ACT.MISSING_PARTICIPANTS");
        assertThat(value(missing).value()).isEqualByComparingTo("1");
    }

    @Test
    void FR_037_passRate_byPerformer_checkedItemsByRole() {
        checks.add(new QualityCheckStatisticsView(1, 9, 101, 71, "PASS", "FLOOR_MANAGER", 51L, at(600)));
        checks.add(new QualityCheckStatisticsView(2, 9, 101, 72, "FAIL", "FLOOR_MANAGER", 51L, at(610)));
        checks.add(new QualityCheckStatisticsView(3, 9, 101, 73, "PASS", "SHIFT_LEAD", 52L, at(620)));
        checks.add(new QualityCheckStatisticsView(4, 9, 101, 74, "NOT_CHECKABLE", "SHIFT_LEAD", 52L, at(630)));
        checks.add(new QualityCheckStatisticsView(5, 9, 101, 75, "OVERDUE", null, 52L, null));
        checks.add(new QualityCheckStatisticsView(6, 9, 101, 76, "PENDING", null, 52L, null));

        PassRateMetric pass = new PassRateMetric(qualityRecords);
        MetricValue v = value(pass);
        assertThat(pass.metric().code()).isEqualTo("QC.PASS_RATE");
        assertThat(pass.metric().group()).isEqualTo(MetricGroup.QUALITY_CHECKS);
        assertThat(v.numerator()).isEqualByComparingTo("2");
        assertThat(v.denominator()).isEqualByComparingTo("3");
        assertThat(cells(v)).containsExactly(tuple("51", "50"), tuple("52", "100"));

        ChecksByRoleMetric byRole = new ChecksByRoleMetric(qualityRecords);
        assertThat(byRole.metric().code()).isEqualTo("QC.BY_CHECKER_ROLE");
        assertThat(cells(value(byRole))).containsExactly(tuple("FLOOR_MANAGER", "2"), tuple("SHIFT_LEAD", "2"));
    }
}
