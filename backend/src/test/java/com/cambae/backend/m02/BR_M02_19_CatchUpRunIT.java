package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m02.internal.transition.service.M02DailyRunner;
import com.cambae.backend.m02.internal.transition.service.M02DailyTask;
import com.cambae.backend.m02.scheduling.M02DailyJob;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 004 T012, research T-11 (FR-077, BR-M02-19): job hằng ngày chạy bù từng ngày theo thứ tự, mỗi việc mỗi ngày đúng một
 * lần; việc chỉ có nghĩa với ngày mới nhất ghi "bỏ qua" cho ngày lỡ; việc lỗi không chặn việc khác và được làm lại; job
 * chạy từ 00:15.
 */
@Import(BR_M02_19_CatchUpRunIT.Tasks.class)
class BR_M02_19_CatchUpRunIT extends MySqlIntegrationTestBase {

    static final String EVERY_DAY = "TEST_EVERY_DAY";
    static final String LATEST_ONLY = "TEST_LATEST_ONLY";
    static final String FLAKY = "TEST_FLAKY";

    @TestConfiguration
    static class Tasks {
        static final Map<String, List<LocalDate>> RUNS = new ConcurrentHashMap<>();
        static final AtomicBoolean FLAKY_FAILS = new AtomicBoolean();

        private static void record(String task, LocalDate day) {
            RUNS.computeIfAbsent(task, k -> new CopyOnWriteArrayList<>()).add(day);
        }

        @Bean
        M02DailyTask everyDayTask() {
            return new M02DailyTask() {
                @Override
                public String name() {
                    return EVERY_DAY;
                }

                @Override
                public int run(LocalDate day) {
                    record(EVERY_DAY, day);
                    return 1;
                }
            };
        }

        @Bean
        M02DailyTask latestOnlyTask() {
            return new M02DailyTask() {
                @Override
                public String name() {
                    return LATEST_ONLY;
                }

                @Override
                public boolean latestDayOnly() {
                    return true;
                }

                @Override
                public int run(LocalDate day) {
                    record(LATEST_ONLY, day);
                    return 1;
                }
            };
        }

        @Bean
        M02DailyTask flakyTask() {
            return new M02DailyTask() {
                @Override
                public String name() {
                    return FLAKY;
                }

                @Override
                public int run(LocalDate day) {
                    if (FLAKY_FAILS.get()) {
                        throw new IllegalStateException("việc lỗi tạm thời");
                    }
                    record(FLAKY, day);
                    return 1;
                }
            };
        }
    }

    @Autowired
    M02DailyRunner runner;

    @Autowired
    M02DailyJob job;

    @Autowired
    TestClockControl clock;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() throws SQLException {
        Tasks.RUNS.clear();
        Tasks.FLAKY_FAILS.set(false);
        // Tài khoản ứng dụng không có DELETE trên daily_run
        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            statement.executeUpdate("DELETE FROM daily_run WHERE task LIKE 'TEST\\_%'");
        }
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private void at(String time) {
        clock.setTo(OffsetDateTime.parse(time).toInstant());
    }

    private List<LocalDate> runs(String task) {
        return Tasks.RUNS.getOrDefault(task, List.of());
    }

    private String result(String task, String day) {
        return jdbc
                .query(
                        "SELECT result FROM daily_run WHERE task = ? AND `date` = ?",
                        (rs, i) -> rs.getString(1),
                        task,
                        day)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Test
    void BR_M02_19_threeSilentDays_eachTaskRunsOncePerMissedDay_inDateOrder() {
        at("2030-03-04T08:00:00+07:00");
        runner.catchUp();
        assertThat(runs(EVERY_DAY)).containsExactly(LocalDate.of(2030, 3, 4));

        // Job tắt 3 ngày (5, 6, 7 tháng 3) rồi chạy lại
        at("2030-03-07T08:00:00+07:00");
        runner.catchUp();

        assertThat(runs(EVERY_DAY))
                .containsExactly(
                        LocalDate.of(2030, 3, 4),
                        LocalDate.of(2030, 3, 5),
                        LocalDate.of(2030, 3, 6),
                        LocalDate.of(2030, 3, 7));
        assertThat(runs(FLAKY)).hasSameElementsAs(runs(EVERY_DAY));
    }

    @Test
    void BR_M02_19_latestDayOnlyTask_runsForNewestDay_andMarksMissedDaysSkipped() {
        at("2030-03-04T08:00:00+07:00");
        runner.catchUp();
        at("2030-03-07T08:00:00+07:00");
        runner.catchUp();

        assertThat(runs(LATEST_ONLY)).containsExactly(LocalDate.of(2030, 3, 4), LocalDate.of(2030, 3, 7));
        assertThat(result(LATEST_ONLY, "2030-03-05")).isEqualTo("SKIPPED");
        assertThat(result(LATEST_ONLY, "2030-03-06")).isEqualTo("SKIPPED");
        assertThat(result(LATEST_ONLY, "2030-03-07")).isEqualTo("SUCCEEDED");
        assertThat(result(EVERY_DAY, "2030-03-05")).isEqualTo("SUCCEEDED");
    }

    @Test
    void BR_M02_19_runningAgain_doesNotRepeatAnyTaskOrDay() {
        at("2030-03-04T08:00:00+07:00");
        runner.catchUp();
        at("2030-03-07T08:00:00+07:00");
        runner.catchUp();
        int everyDayRuns = runs(EVERY_DAY).size();

        runner.catchUp();
        at("2030-03-07T23:30:00+07:00");
        runner.catchUp();

        assertThat(runs(EVERY_DAY)).hasSize(everyDayRuns);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM daily_run WHERE task = ? AND `date` = '2030-03-07'",
                        Integer.class,
                        EVERY_DAY))
                .isEqualTo(1);
    }

    @Test
    void BR_M02_19_recordsSchedulerRun_andLinksEachDayToIt() {
        at("2030-03-10T08:00:00+07:00");
        runner.catchUp();

        Long runId = jdbc.queryForObject(
                "SELECT scheduler_run_id FROM daily_run WHERE task = ? AND `date` = '2030-03-10'",
                Long.class,
                EVERY_DAY);
        assertThat(jdbc.queryForObject("SELECT job FROM scheduler_run WHERE id = ?", String.class, runId))
                .isEqualTo("M02_DAILY");
        assertThat(jdbc.queryForObject("SELECT result FROM scheduler_run WHERE id = ?", String.class, runId))
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void FR_077_beforeQuarterPastMidnight_todayIsNotProcessedYet() {
        at("2030-04-10T00:10:00+07:00");
        runner.catchUp();
        assertThat(runs(EVERY_DAY)).containsExactly(LocalDate.of(2030, 4, 9));

        at("2030-04-10T00:15:00+07:00");
        runner.catchUp();
        assertThat(runs(EVERY_DAY)).containsExactly(LocalDate.of(2030, 4, 9), LocalDate.of(2030, 4, 10));
    }

    @Test
    void BR_M02_19_failingTask_doesNotBlockOthers_andIsRetriedFromTheFailedDay() {
        at("2030-05-01T08:00:00+07:00");
        runner.catchUp();
        Tasks.FLAKY_FAILS.set(true);
        at("2030-05-04T08:00:00+07:00");
        runner.catchUp();

        assertThat(runs(EVERY_DAY))
                .containsExactly(
                        LocalDate.of(2030, 5, 1),
                        LocalDate.of(2030, 5, 2),
                        LocalDate.of(2030, 5, 3),
                        LocalDate.of(2030, 5, 4));
        assertThat(runs(FLAKY)).containsExactly(LocalDate.of(2030, 5, 1));
        assertThat(result(FLAKY, "2030-05-02")).isNull();
        Long failedRun = jdbc.queryForObject(
                "SELECT scheduler_run_id FROM daily_run WHERE task = ? AND `date` = '2030-05-04'",
                Long.class,
                EVERY_DAY);
        assertThat(jdbc.queryForObject("SELECT result FROM scheduler_run WHERE id = ?", String.class, failedRun))
                .isEqualTo("FAILED");

        Tasks.FLAKY_FAILS.set(false);
        runner.catchUp();

        assertThat(runs(FLAKY))
                .containsExactly(
                        LocalDate.of(2030, 5, 1),
                        LocalDate.of(2030, 5, 2),
                        LocalDate.of(2030, 5, 3),
                        LocalDate.of(2030, 5, 4));
        assertThat(runs(EVERY_DAY)).hasSize(4);
    }

    @Test
    void NFR_13_job_isNamedForTestClock_andRunNowCatchesUp() {
        assertThat(job.name()).isEqualTo("M02_DAILY");

        at("2030-07-02T08:00:00+07:00");
        job.runNow();
        job.runNow();

        assertThat(runs(EVERY_DAY)).containsExactly(LocalDate.of(2030, 7, 2));
    }

    @Test
    void BR_M02_19_twoRunnersAtOnce_stillRunEachTaskOncePerDay() throws Exception {
        at("2030-06-01T08:00:00+07:00");
        runner.catchUp();
        at("2030-06-05T08:00:00+07:00");
        CountDownLatch start = new CountDownLatch(1);
        Runnable race = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            runner.catchUp();
        };
        Thread first = new Thread(race);
        Thread second = new Thread(race);
        first.start();
        second.start();
        start.countDown();
        first.join(30_000);
        second.join(30_000);

        assertThat(runs(EVERY_DAY))
                .containsExactlyInAnyOrder(
                        LocalDate.of(2030, 6, 1),
                        LocalDate.of(2030, 6, 2),
                        LocalDate.of(2030, 6, 3),
                        LocalDate.of(2030, 6, 4),
                        LocalDate.of(2030, 6, 5));
    }
}
