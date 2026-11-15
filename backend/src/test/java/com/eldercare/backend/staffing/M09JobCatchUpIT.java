package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.TestClockControl;
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
    /** Chạy qua {@code ScheduledJob.runNow} để người dùng hiện tại là Bộ lập lịch như job thật (008 T057). */
    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minuteJob;

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
    void FR_022_failedTickCanBeRetriedThenBecomesIdempotent() {
        clock.setTo(Instant.parse("2026-10-23T05:20:20Z"));
        minute.fail = true;
        assertThatThrownBy(() -> minuteJob.runNow()).isInstanceOf(IllegalStateException.class);
        assertThat(marked("M09_MINUTE", "2026-10-23T05:20:00Z")).isFalse();
        minute.fail = false;
        minuteJob.runNow();
        int after = minute.calls.size();
        minuteJob.runNow();
        assertThat(minute.calls).hasSize(after);
        assertThat(marked("M09_MINUTE", "2026-10-23T05:20:00Z")).isTrue();
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
