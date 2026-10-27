package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m02.internal.registration.domain.ReminderType;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationReminderStore;
import com.cambae.backend.m02.internal.registration.service.RegistrationDailyRunner;
import com.cambae.backend.m02.internal.registration.service.RegistrationDailyTask;
import com.cambae.backend.m02.scheduling.ResidenceRegistrationJob;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 018 T004 ({@code ChayBuM02KhaiBaoIT}; research Z-06, NFR-04, Q-306): job khai báo chạy từ 06:00, chạy bù lúc khởi động
 * cho ngày mới nhất chưa chạy, mỗi việc mỗi ngày đúng một lần kể cả khi chạy lại hay hai tiến trình chạy cùng lúc; việc lỗi
 * không chặn việc khác và được làm lại; khóa nhắc {@code (loại, người, ngày)} không cho nhắc trùng.
 */
@Import(RegistrationCatchUpIT.Tasks.class)
@TestPropertySource(
        properties = {"eldercare.jobs.m02-registration.cron=-", "eldercare.jobs.m02-registration.on-startup=false"})
class RegistrationCatchUpIT extends MySqlIntegrationTestBase {

    static final String STEADY = "TEST_REG_STEADY";
    static final String FLAKY = "TEST_REG_FLAKY";
    static final long RESIDENT = 918_500_001L;

    @TestConfiguration
    static class Tasks {
        static final Map<String, List<LocalDate>> RUNS = new ConcurrentHashMap<>();
        static final AtomicBoolean FLAKY_FAILS = new AtomicBoolean();

        private static void record(String task, LocalDate day) {
            RUNS.computeIfAbsent(task, k -> new CopyOnWriteArrayList<>()).add(day);
        }

        /** Việc mẫu: nhắc quá hạn một người, qua khóa nhắc. */
        @Bean
        RegistrationDailyTask steadyTask(RegistrationReminderStore reminders) {
            return new RegistrationDailyTask() {
                @Override
                public String name() {
                    return STEADY;
                }

                @Override
                public int run(LocalDate day) {
                    record(STEADY, day);
                    return reminders.claim(ReminderType.OVERDUE, RESIDENT, day) ? 1 : 0;
                }
            };
        }

        @Bean
        RegistrationDailyTask flakyTask() {
            return new RegistrationDailyTask() {
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
    RegistrationDailyRunner runner;

    @Autowired
    ResidenceRegistrationJob job;

    @Autowired
    RegistrationReminderStore reminders;

    @Autowired
    TestClockControl clock;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @BeforeEach
    void clean() throws SQLException {
        Tasks.RUNS.clear();
        Tasks.FLAKY_FAILS.set(false);
        // Tài khoản ứng dụng không có DELETE trên daily_run, registration_reminder
        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            statement.executeUpdate("DELETE FROM daily_run WHERE task LIKE 'TEST\\_REG\\_%'");
            statement.executeUpdate("DELETE FROM registration_reminder WHERE resident_id = " + RESIDENT);
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

    private int reminderRows() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM registration_reminder WHERE resident_id = ?", Integer.class, RESIDENT);
    }

    @Test
    void Z_06_runningAgainTheSameDay_runsEachTaskOnce_andRemindsOnce() {
        at("2030-03-04T07:00:00+07:00");

        runner.catchUp();
        runner.catchUp();
        job.runNow();

        assertThat(runs(STEADY)).containsExactly(LocalDate.of(2030, 3, 4));
        assertThat(runs(FLAKY)).containsExactly(LocalDate.of(2030, 3, 4));
        assertThat(reminderRows()).isOne();
    }

    @Test
    void Z_06_beforeSixOClock_handlesYesterday_fromSixOClock_today() {
        at("2030-03-05T05:59:59+07:00");
        runner.catchUp();
        at("2030-03-05T06:00:00+07:00");
        runner.catchUp();

        assertThat(runs(STEADY)).containsExactly(LocalDate.of(2030, 3, 4), LocalDate.of(2030, 3, 5));
    }

    /** Việc của 018 xét trạng thái tại ngày chạy: sau khi job tắt nhiều ngày, lần chạy đầu tiên xử lý ngày mới nhất. */
    @Test
    void Z_06_afterSilentDays_firstRunHandlesTheLatestDay() {
        at("2030-04-01T07:00:00+07:00");
        runner.catchUp();
        at("2030-04-04T06:30:00+07:00");
        runner.catchUp();

        assertThat(runs(STEADY)).containsExactly(LocalDate.of(2030, 4, 1), LocalDate.of(2030, 4, 4));
        assertThat(reminderRows()).isEqualTo(2);
    }

    @Test
    void Z_06_failingTask_doesNotBlockOthers_andIsRetried() {
        at("2030-05-01T07:00:00+07:00");
        Tasks.FLAKY_FAILS.set(true);
        runner.catchUp();

        assertThat(runs(STEADY)).containsExactly(LocalDate.of(2030, 5, 1));
        assertThat(runs(FLAKY)).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM daily_run WHERE task = ? AND `date` = '2030-05-01'",
                        Integer.class,
                        FLAKY))
                .isZero();

        Tasks.FLAKY_FAILS.set(false);
        runner.catchUp();

        assertThat(runs(FLAKY)).containsExactly(LocalDate.of(2030, 5, 1));
        assertThat(runs(STEADY)).hasSize(1);
    }

    @Test
    void NFR_04_twoRunnersAtOnce_stillRunEachTaskOncePerDay() throws Exception {
        at("2030-06-01T07:00:00+07:00");
        CountDownLatch start = new CountDownLatch(1);
        Runnable race = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            runner.catchUp();
        };
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> a = pool.submit(race);
            Future<?> b = pool.submit(race);
            start.countDown();
            a.get();
            b.get();
        }

        assertThat(runs(STEADY)).containsExactly(LocalDate.of(2030, 6, 1));
        assertThat(reminderRows()).isOne();
    }

    @Test
    void Z_06_reminderKey_claimsOncePerTypeResidentAndDay() {
        LocalDate day = LocalDate.of(2030, 7, 1);

        List<Boolean> claims = transaction.execute(status -> List.of(
                reminders.claim(ReminderType.OVERDUE, RESIDENT, day),
                reminders.claim(ReminderType.OVERDUE, RESIDENT, day),
                reminders.claim(ReminderType.RENEWAL, RESIDENT, day),
                reminders.claim(ReminderType.OVERDUE, RESIDENT, day.plusDays(1))));

        assertThat(claims).containsExactly(true, false, true, true);
    }

    @Test
    void NFR_13_job_isNamedForTestClock_locked_andScheduledAtSixInHoChiMinh() throws Exception {
        assertThat(job.name()).isEqualTo("M02_REGISTRATION");
        var scheduled = ResidenceRegistrationJob.class.getMethod("scheduled");
        assertThat(scheduled.getAnnotation(SchedulerLock.class).name()).isEqualTo("M02_REGISTRATION");
        assertThat(scheduled.getAnnotation(Scheduled.class).cron()).endsWith(":0 0 6 * * *}");
        assertThat(scheduled.getAnnotation(Scheduled.class).zone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(ResidenceRegistrationJob.class
                        .getMethod("atStartup")
                        .getAnnotation(SchedulerLock.class)
                        .name())
                .isEqualTo("M02_REGISTRATION");
    }
}
