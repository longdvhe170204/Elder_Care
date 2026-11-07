package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.staffing.internal.service.M09Jobs;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.internal.service.M09TimedTask.Cadence;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** T008/T050: mốc thành công ở ReminderMarkers; lỗi rollback để retry, chi tiết lần chạy ra SchedulerRuns. */
@Import(M09JobCatchUpIT.Tasks.class)
class M09JobCatchUpIT extends MySqlIntegrationTestBase {
    @Autowired
    M09Jobs jobs;

    @Autowired
    TestClockControl clock;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ReminderMarkers markers;

    @Autowired
    @Qualifier("minuteTask")
    CounterTask minute;

    @Autowired
    @Qualifier("dailyTask")
    CounterTask daily;

    @AfterEach
    void resetClock() {
        clock.reset();
        minute.fail = false;
    }

    private boolean marked(String job, String at) {
        return markers.isMarked("M09.JOB." + job, "M09.JOB", 0, at);
    }

    @Test
    void Q_306_minuteCatchUpRunsOncePerTickWithoutCreatingStaff() {
        clock.setTo(Instant.parse("2026-10-23T03:40:25Z"));
        int staff = jdbc.queryForObject("SELECT COUNT(*) FROM staff_profile", Integer.class);
        int before = minute.calls.size();
        jobs.runDue(Cadence.MINUTE);
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).hasSize(before + 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_profile", Integer.class))
                .isEqualTo(staff);
        assertThat(marked("M09_MINUTE", "2026-10-23T03:40:00Z")).isTrue();
    }

    @Test
    void FR_008_dailyCatchUpUses0010VietnamTimeAndKeepsScheduledAndActualTimes() {
        clock.setTo(Instant.parse("2026-10-23T18:00:00Z"));
        int before = daily.calls.size();
        jobs.runDue(Cadence.DAILY);
        jobs.runDue(Cadence.DAILY);
        assertThat(daily.calls).hasSize(before + 1);
        assertThat(daily.calls.getLast()).isEqualTo(Instant.parse("2026-10-23T17:10:00Z"));
        assertThat(daily.actuals.getLast()).isEqualTo(Instant.parse("2026-10-23T18:00:00Z"));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(details->'$.actualAt') FROM reminder_marker WHERE kind='M09.JOB.M09_DAILY' AND marker='2026-10-23T17:10:00Z'",
                        String.class))
                .isEqualTo("2026-10-23T18:00:00Z");
    }

    @Test
    void FR_022_failedTickCanBeRetriedThenBecomesIdempotent() {
        clock.setTo(Instant.parse("2026-10-23T05:20:20Z"));
        minute.fail = true;
        assertThatThrownBy(() -> jobs.runDue(Cadence.MINUTE)).isInstanceOf(IllegalStateException.class);
        assertThat(marked("M09_MINUTE", "2026-10-23T05:20:00Z")).isFalse();
        minute.fail = false;
        jobs.runDue(Cadence.MINUTE);
        int after = minute.calls.size();
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).hasSize(after);
        assertThat(marked("M09_MINUTE", "2026-10-23T05:20:00Z")).isTrue();
    }

    @Test
    void FR_045_jobUsesUuidAndRestoresCallerMdcEvenWhenItFails() {
        String previous = MDC.get(SchedulerRuns.MDC_KEY);
        try {
            MDC.put(SchedulerRuns.MDC_KEY, "caller");
            clock.setTo(Instant.parse("2026-10-23T07:20:20Z"));
            jobs.runDue(Cadence.MINUTE);
            String runId = minute.runIds.getLast();
            assertThat(runId).isNotNull().isNotEqualTo("caller");
            assertThat(UUID.fromString(runId).toString()).isEqualTo(runId);
            assertThat(MDC.get(SchedulerRuns.MDC_KEY)).isEqualTo("caller");
            clock.setTo(Instant.parse("2026-10-23T07:21:20Z"));
            minute.fail = true;
            assertThatThrownBy(() -> jobs.runDue(Cadence.MINUTE)).isInstanceOf(IllegalStateException.class);
            assertThat(MDC.get(SchedulerRuns.MDC_KEY)).isEqualTo("caller");
        } finally {
            if (previous == null) MDC.remove(SchedulerRuns.MDC_KEY);
            else MDC.put(SchedulerRuns.MDC_KEY, previous);
        }
    }

    @Test
    void FR_008_concurrentTicksCommitOneMarkerAndExecuteOneTask() throws Exception {
        clock.setTo(Instant.parse("2026-10-23T09:20:20Z"));
        int before = minute.calls.size();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> {
                start.await();
                jobs.runDue(Cadence.MINUTE);
                return null;
            });
            var b = pool.submit(() -> {
                start.await();
                jobs.runDue(Cadence.MINUTE);
                return null;
            });
            start.countDown();
            a.get(30, TimeUnit.SECONDS);
            b.get(30, TimeUnit.SECONDS);
        }
        assertThat(minute.calls).hasSize(before + 1);
        assertThat(marked("M09_MINUTE", "2026-10-23T09:20:00Z")).isTrue();
    }

    static class CounterTask implements M09TimedTask {
        private final Cadence cadence;
        private final List<Instant> calls = new ArrayList<>();
        private final List<Instant> actuals = new ArrayList<>();
        private final List<String> runIds = new ArrayList<>();
        private boolean fail;

        CounterTask(Cadence cadence) {
            this.cadence = cadence;
        }

        public Cadence cadence() {
            return cadence;
        }

        public int run(Instant scheduledAt, Instant actualAt) {
            calls.add(scheduledAt);
            actuals.add(actualAt);
            runIds.add(MDC.get(SchedulerRuns.MDC_KEY));
            if (fail) throw new IllegalStateException("Lỗi tác vụ kiểm thử");
            return 1;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Tasks {
        @Bean
        CounterTask minuteTask() {
            return new CounterTask(Cadence.MINUTE);
        }

        @Bean
        CounterTask dailyTask() {
            return new CounterTask(Cadence.DAILY);
        }
    }
}
