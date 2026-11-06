package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import com.eldercare.backend.billing.internal.period.domain.CostJobCadence;
import com.eldercare.backend.billing.internal.period.service.CostTimedTask;
import com.eldercare.backend.billing.scheduling.M11DailyJob;
import com.eldercare.backend.billing.scheduling.M11HourlyJob;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** T007: khung chạy bù/ghi dấu thật; tác vụ mẫu chỉ có trong test, không thay tác vụ nghiệp vụ T016/T027. */
@Import(CostJobCatchUpIT.Actions.class)
class CostJobCatchUpIT extends MySqlIntegrationTestBase {
    @Autowired
    M11DailyJob daily;

    @Autowired
    M11HourlyJob hourly;

    @Autowired
    TestClockControl clock;

    @Autowired
    ResidentModuleStateApi states;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate calendarJdbc;

    @Autowired
    @Qualifier("dailyCostTestAction")
    Action dailyAction;

    @Autowired
    @Qualifier("hourlyCostTestAction")
    Action hourlyAction;

    private final Instant first =
            LocalDate.of(2026, 11, 4).atTime(0, 20).atZone(BusinessTime.ZONE).toInstant();

    @BeforeEach
    void setup() {
        dailyAction.reset();
        hourlyAction.reset();
        clock.setTo(first);
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    @Test
    void FR_001_realCalendarTaskCommitsItsPeriodThroughTheScheduler() {
        int statementsBefore = calendarJdbc.queryForObject(
                "SELECT COUNT(*) FROM cost_statement WHERE kind='STATEMENT'", Integer.class);
        daily.runNow();
        assertThat(calendarJdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_statement WHERE kind='PERIOD' AND period_month='2026-11'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(calendarJdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_statement WHERE kind='STATEMENT'", Integer.class))
                .isEqualTo(statementsBefore);
        assertThat(calendarJdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.JOB_DONE' AND entity_type=? AND entity_id=? AND marker=?",
                        Integer.class,
                        com.eldercare.backend.billing.internal.period.persistence.CostJobMarkers.entityType(
                                "COST_PERIOD_CALENDAR"),
                        CostJobCadence.DAILY.markerId(),
                        first.toString()))
                .isEqualTo(1);
    }

    @Test
    void NFR_04_dailyJobCatchesUpEveryMissedDayAndDoesNotRepeatCompletedSlots() {
        daily.runNow();
        clock.setTo(first.plus(Duration.ofDays(3)));
        daily.runNow();
        daily.runNow();
        assertThat(dailyAction.calls)
                .containsExactly(
                        first,
                        first.plus(Duration.ofDays(1)),
                        first.plus(Duration.ofDays(2)),
                        first.plus(Duration.ofDays(3)));
    }

    @Test
    void NFR_04_dailyJobBefore0020DoesNotRunTheCurrentDayEarly() {
        clock.setTo(first.minusSeconds(1));
        daily.runNow();
        assertThat(dailyAction.calls).containsExactly(first.minus(Duration.ofDays(1)));
        clock.setTo(first);
        daily.runNow();
        assertThat(dailyAction.calls).containsExactly(first.minus(Duration.ofDays(1)), first);
    }

    @Test
    void NFR_04_hourlyJobReplaysHoursIndependentlyOfDailyMarkers() {
        hourly.runNow();
        clock.setTo(first.plus(Duration.ofHours(3)));
        hourly.runNow();
        daily.runNow();
        Instant hour = first.minus(Duration.ofMinutes(20));
        assertThat(hourlyAction.calls)
                .containsExactly(hour, hour.plusSeconds(3600), hour.plusSeconds(7200), hour.plusSeconds(10800));
        assertThat(dailyAction.calls).containsExactly(first);
    }

    @Test
    void NFR_04_latestOnlyActionsSkipOldRemindersWhileRecordingTheSkippedSlots() {
        dailyAction.latestOnly = true;
        daily.runNow();
        clock.setTo(first.plus(Duration.ofDays(3)));
        daily.runNow();
        daily.runNow();
        assertThat(dailyAction.calls).containsExactly(first, first.plus(Duration.ofDays(3)));
    }

    @Test
    void NFR_04_failedActionRollsBackItsSharedStateAndMarkerThenRetries() {
        long resident = TestUsers.nextId();
        dailyAction.work = slot -> {
            states.save(BillingResidentState.KIND, resident, new BillingResidentState(slot));
            throw new IllegalStateException("Lỗi nguồn mẫu để kiểm rollback");
        };
        daily.runNow();
        assertThat(states.get(BillingResidentState.KIND, resident)).isEmpty();
        dailyAction.work = slot -> {
            states.save(BillingResidentState.KIND, resident, new BillingResidentState(slot));
            return 1;
        };
        daily.runNow();
        daily.runNow();
        assertThat(dailyAction.calls).containsExactly(first, first);
        assertThat(states.get(BillingResidentState.KIND, resident)).contains(new BillingResidentState(first));
    }

    @Test
    void NFR_04_failedFirstSlotIsNotForgottenWhenTheClockAdvances() {
        dailyAction.work = slot -> {
            throw new IllegalStateException("Lỗi lượt đầu");
        };
        daily.runNow();
        dailyAction.work = slot -> 1;
        clock.setTo(first.plus(Duration.ofDays(2)));
        daily.runNow();
        assertThat(dailyAction.calls)
                .containsExactly(first, first, first.plus(Duration.ofDays(1)), first.plus(Duration.ofDays(2)));
    }

    @Test
    void NFR_04_twoManualRunsExecuteTheSameSlotOnlyOnce() throws Exception {
        var start = new CyclicBarrier(2);
        Callable<Void> run = () -> {
            start.await(10, TimeUnit.SECONDS);
            daily.runNow();
            return null;
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(run);
            var right = executor.submit(run);
            left.get(20, TimeUnit.SECONDS);
            right.get(20, TimeUnit.SECONDS);
        }
        assertThat(dailyAction.calls).containsExactly(first);
    }

    @Test
    void NFR_04_testProfileDoesNotRunEitherJobAtStartup() {
        daily.atStartup();
        hourly.atStartup();
        assertThat(dailyAction.calls).isEmpty();
        assertThat(hourlyAction.calls).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Actions {
        @Bean
        Action dailyCostTestAction() {
            return new Action(CostJobCadence.DAILY);
        }

        @Bean
        Action hourlyCostTestAction() {
            return new Action(CostJobCadence.HOURLY);
        }
    }

    static class Action implements CostTimedTask {
        final CostJobCadence cadence;
        final List<Instant> calls = new CopyOnWriteArrayList<>();
        String name;
        boolean latestOnly;
        ToIntFunction<Instant> work;

        Action(CostJobCadence cadence) {
            this.cadence = cadence;
            reset();
        }

        void reset() {
            name = "TEST_" + TestUsers.nextId();
            calls.clear();
            latestOnly = false;
            work = slot -> 1;
        }

        public String name() {
            return name;
        }

        public CostJobCadence cadence() {
            return cadence;
        }

        public boolean latestOnly() {
            return latestOnly;
        }

        public int run(Instant slot) {
            calls.add(slot);
            return work.applyAsInt(slot);
        }
    }
}
