package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.internal.scheduling.service.M10Jobs;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask.Cadence;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 012 T007 ({@code ChayBuM10IT}; research F-17, NFR-04): khung việc theo phút, theo giờ, nửa đêm của M10 chống chạy trùng
 * theo mốc bằng dấu ở {@code reminder_marker} (R-15), chạy lại mốc lỗi, chạy bù từng đêm bị lỡ. Chưa có tác vụ nghiệp vụ;
 * tên tác vụ kiểm thử đổi mỗi lần chạy vì schema test giữ dấu qua các lần.
 */
@Import(M10JobCatchUpIT.Tasks.class)
class M10JobCatchUpIT extends MySqlIntegrationTestBase {

    @Autowired
    M10Jobs jobs;

    @Autowired
    TestClockControl clock;

    @Autowired
    @Qualifier("minuteTask")
    CounterTask minute;

    @Autowired
    @Qualifier("otherMinuteTask")
    CounterTask otherMinute;

    @Autowired
    @Qualifier("hourlyTask")
    CounterTask hourly;

    @Autowired
    @Qualifier("midnightTask")
    CounterTask midnight;

    @AfterEach
    void reset() {
        clock.reset();
        minute.fail = false;
    }

    @Test
    void NFR_04_minuteTask_runsOncePerMinute() {
        clock.setTo(Instant.parse("2026-11-04T03:40:25Z"));
        int before = minute.calls.size();
        jobs.runDue(Cadence.MINUTE);
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).hasSize(before + 1);
        assertThat(minute.calls.getLast()).isEqualTo(Instant.parse("2026-11-04T03:40:00Z"));

        clock.setTo(Instant.parse("2026-11-04T03:41:05Z"));
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).hasSize(before + 2);
    }

    @Test
    void F_17_failedTask_isRetriedAndDoesNotBlockOtherTasks() {
        clock.setTo(Instant.parse("2026-11-04T05:20:20Z"));
        minute.fail = true;
        int before = minute.calls.size();
        int otherBefore = otherMinute.calls.size();
        jobs.runDue(Cadence.MINUTE);
        assertThat(otherMinute.calls).hasSize(otherBefore + 1);

        minute.fail = false;
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).as("mốc lỗi chạy lại").hasSize(before + 2);
        assertThat(otherMinute.calls).as("mốc đã xong không chạy lại").hasSize(otherBefore + 1);
        jobs.runDue(Cadence.MINUTE);
        assertThat(minute.calls).hasSize(before + 2);
    }

    @Test
    void F_17_hourlyTask_usesTheStartOfTheHour() {
        clock.setTo(Instant.parse("2026-11-04T07:59:59Z"));
        int before = hourly.calls.size();
        jobs.runDue(Cadence.HOURLY);
        jobs.runDue(Cadence.HOURLY);
        assertThat(hourly.calls).hasSize(before + 1);
        assertThat(hourly.calls.getLast()).isEqualTo(Instant.parse("2026-11-04T07:00:00Z"));
    }

    @Test
    void F_17_midnightTask_catchesUpEveryMissedNightInOrder() {
        // 00:30 ngày 05/11 giờ Việt Nam; lần đầu chỉ chạy đêm mới nhất
        clock.setTo(Instant.parse("2026-11-04T17:30:00Z"));
        int before = midnight.calls.size();
        jobs.runDue(Cadence.MIDNIGHT);
        assertThat(midnight.calls.subList(before, midnight.calls.size()))
                .containsExactly(Instant.parse("2026-11-04T17:00:00Z"));

        // ứng dụng dừng ba ngày, khởi động lúc 09:00 ngày 08/11
        clock.setTo(Instant.parse("2026-11-08T02:00:00Z"));
        jobs.runDue(Cadence.MIDNIGHT);
        assertThat(midnight.calls.subList(before + 1, midnight.calls.size()))
                .containsExactly(
                        Instant.parse("2026-11-05T17:00:00Z"),
                        Instant.parse("2026-11-06T17:00:00Z"),
                        Instant.parse("2026-11-07T17:00:00Z"));
        jobs.runDue(Cadence.MIDNIGHT);
        assertThat(midnight.calls).hasSize(before + 4);
    }

    static class CounterTask implements M10TimedTask {
        private final String name = "TEST_" + UUID.randomUUID().toString().substring(0, 13);
        private final Cadence cadence;
        private final List<Instant> calls = new ArrayList<>();
        private boolean fail;

        CounterTask(Cadence cadence) {
            this.cadence = cadence;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Cadence cadence() {
            return cadence;
        }

        @Override
        public int run(Instant scheduledAt, Instant actualAt) {
            calls.add(scheduledAt);
            if (fail) {
                throw new IllegalStateException("Lỗi tác vụ kiểm thử");
            }
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
        CounterTask otherMinuteTask() {
            return new CounterTask(Cadence.MINUTE);
        }

        @Bean
        CounterTask hourlyTask() {
            return new CounterTask(Cadence.HOURLY);
        }

        @Bean
        CounterTask midnightTask() {
            return new CounterTask(Cadence.MIDNIGHT);
        }
    }
}
