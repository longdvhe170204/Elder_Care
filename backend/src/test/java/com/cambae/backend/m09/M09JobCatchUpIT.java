package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m09.internal.service.M09Jobs;
import com.cambae.backend.m09.internal.service.M09TimedTask;
import com.cambae.backend.m09.internal.service.M09TimedTask.Cadence;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** T008: khung chạy bù, bỏ trùng và thử lại trên MySQL; chưa có tác vụ nghiệp vụ nào của story. */
@Import(M09JobCatchUpIT.Tasks.class)
class M09JobCatchUpIT extends MySqlIntegrationTestBase {
    @Autowired
    M09Jobs jobs;

    @Autowired
    TestClockControl clock;

    @Autowired
    JdbcTemplate jdbc;

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
        assertThat(jdbc.queryForObject(
                        "SELECT result FROM m09_job_run WHERE job='M09_MINUTE' AND scheduled_at='2026-10-23 03:40:00'",
                        String.class))
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void FR_008_dailyCatchUpUses0010VietnamTimeAndKeepsScheduledAndActualTimes() {
        clock.setTo(Instant.parse("2026-10-23T18:00:00Z"));
        int before = daily.calls.size();
        jobs.runDue(Cadence.DAILY);
        jobs.runDue(Cadence.DAILY);
        assertThat(daily.calls).hasSize(before + 1);
        assertThat(daily.calls.getLast()).isEqualTo(Instant.parse("2026-10-23T17:10:00Z"));
        assertThat(jdbc.queryForObject(
                                "SELECT started_at FROM m09_job_run WHERE job='M09_DAILY' AND scheduled_at='2026-10-23 17:10:00'",
                                LocalDateTime.class)
                        .toInstant(ZoneOffset.UTC))
                .isAfterOrEqualTo(Instant.parse("2026-10-23T18:00:00Z"));
    }

    @Test
    void FR_022_failedTickCanBeRetriedThenBecomesIdempotent() {
        clock.setTo(Instant.parse("2026-10-23T05:20:20Z"));
        minute.fail = true;
        assertThatThrownBy(() -> jobs.runDue(Cadence.MINUTE)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT result FROM m09_job_run WHERE job='M09_MINUTE' AND scheduled_at='2026-10-23 05:20:00'",
                        String.class))
                .isEqualTo("FAILED");
        minute.fail = false;
        jobs.runDue(Cadence.MINUTE);
        int after = minute.calls.size();
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).hasSize(after);
    }

    static class CounterTask implements M09TimedTask {
        private final Cadence cadence;
        private final List<Instant> calls = new ArrayList<>();
        private boolean fail;

        CounterTask(Cadence cadence) {
            this.cadence = cadence;
        }

        public Cadence cadence() {
            return cadence;
        }

        public void run(Instant scheduledAt, Instant actualAt) {
            calls.add(scheduledAt);
            if (fail) throw new IllegalStateException("Lỗi tác vụ kiểm thử");
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
