package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.notification.internal.job.service.M13DailyTask;
import com.eldercare.backend.notification.internal.job.service.M13FastTask;
import com.eldercare.backend.notification.scheduling.M13DailyJob;
import com.eldercare.backend.notification.scheduling.M13FastJob;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * NFR-04, research B-10 (009 T010, {@code ChayBuM13IT}): job hằng ngày 07:30 chạy bù khi khởi động sau giờ chạy mà
 * ngày đó chưa chạy, mỗi ngày một lần; job 20 giây chạy mọi việc nhanh, việc lỗi không chặn việc khác.
 */
@Import(NFR_04_M13CatchUpIT.Tasks.class)
class NFR_04_M13CatchUpIT extends M13IntegrationTestBase {

    @TestConfiguration
    static class Tasks {
        static final List<LocalDate> DAILY_RUNS = new CopyOnWriteArrayList<>();
        static final AtomicInteger FAST_RUNS = new AtomicInteger();

        @Bean
        M13DailyTask countingDailyTask() {
            return new M13DailyTask() {
                @Override
                public String name() {
                    return "COUNTING";
                }

                @Override
                public int run(LocalDate day) {
                    DAILY_RUNS.add(day);
                    return 1;
                }
            };
        }

        @Bean
        M13FastTask failingFastTask() {
            return new M13FastTask() {
                @Override
                public String name() {
                    return "FAILING";
                }

                @Override
                public int run() {
                    throw new IllegalStateException("việc nhanh lỗi");
                }
            };
        }

        @Bean
        M13FastTask countingFastTask() {
            return new M13FastTask() {
                @Override
                public String name() {
                    return "COUNTING";
                }

                @Override
                public int run() {
                    return FAST_RUNS.incrementAndGet();
                }
            };
        }
    }

    @Autowired
    TestClockControl clock;

    @Autowired
    M13DailyJob daily;

    @Autowired
    M13FastJob fast;

    @AfterEach
    void resetClock() {
        clock.reset();
        Tasks.DAILY_RUNS.clear();
    }

    @Test
    void NFR_04_startupAfterRunTime_catchesUpOncePerDay() {
        clock.setTo(OffsetDateTime.parse("2030-03-04T08:15:00+07:00").toInstant());

        daily.catchUp();
        daily.catchUp();

        assertThat(Tasks.DAILY_RUNS).containsExactly(LocalDate.of(2030, 3, 4));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM m13_job_run WHERE job = 'M13_DAILY' AND business_date = '2030-03-04'",
                        Integer.class))
                .isEqualTo(1);
    }

    @Test
    void NFR_04_beforeRunTime_doesNotRun() {
        clock.setTo(OffsetDateTime.parse("2030-03-05T07:29:00+07:00").toInstant());

        daily.catchUp();

        assertThat(Tasks.DAILY_RUNS).isEmpty();
    }

    @Test
    void NFR_04_nextDay_runsAgain() {
        clock.setTo(OffsetDateTime.parse("2030-03-06T07:30:00+07:00").toInstant());
        daily.catchUp();
        clock.setTo(OffsetDateTime.parse("2030-03-07T09:00:00+07:00").toInstant());
        daily.catchUp();

        assertThat(Tasks.DAILY_RUNS).containsExactly(LocalDate.of(2030, 3, 6), LocalDate.of(2030, 3, 7));
    }

    @Test
    void B_10_fastJob_runsEveryTask_evenWhenOneFails() {
        int before = Tasks.FAST_RUNS.get();

        long runId = fast.run();

        assertThat(Tasks.FAST_RUNS.get()).isEqualTo(before + 1);
        assertThat(jdbc.queryForObject("SELECT result FROM scheduler_run WHERE id = ?", String.class, runId))
                .isEqualTo("FAILED");
    }
}
