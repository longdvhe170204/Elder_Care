package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.scheduling.service.*;
import com.eldercare.backend.care.internal.status.service.CareResidentLock;
import com.eldercare.backend.care.scheduling.*;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class NFR_04_CareSchedulingIT extends MySqlIntegrationTestBase {
    @Autowired
    javax.sql.DataSource dataSource;

    @Test
    void FR_006_existingResidentRowDoesNotDeadlockOnLockUpgrade() throws Exception {
        jdbc.update("INSERT INTO care_resident_lock(resident_id) VALUES (99103)");
        var barrier = new CyclicBarrier(2);
        var proxy = net.ttddyy.dsproxy.support.ProxyDataSourceBuilder.create(dataSource)
                .afterQuery((execution, queries) -> {
                    if (queries.stream().anyMatch(q -> q.getQuery().startsWith("INSERT"))) {
                        try {
                            barrier.await(500, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        } catch (BrokenBarrierException | TimeoutException ignored) {
                        }
                    }
                })
                .build();
        var local = new com.eldercare.backend.care.internal.status.persistence.CareLockStore(new JdbcTemplate(proxy));
        var transaction =
                new TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(proxy));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> transaction.executeWithoutResult(tx -> local.lock(List.of(99103L))));
            var two = executor.submit(() -> transaction.executeWithoutResult(tx -> local.lock(List.of(99103L))));
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
        } finally {
            jdbc.update("DELETE FROM care_resident_lock WHERE resident_id=99103");
        }
    }

    @Autowired
    CareResidentLock locks;

    @Autowired
    CareDailyWorkRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager manager;

    @Autowired
    SchedulerRuns runs;

    private final Instant at = Instant.parse("2026-10-27T01:00:00Z");

    @Test
    void FR_006_residentLocksRequireTransactionAndSerializeOverlappingBatches() throws Exception {
        assertThatThrownBy(() -> locks.lock(List.of(99101L)))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var finished = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();
            for (var ids : List.of(List.of(99102L, 99101L), List.of(99101L, 99102L)))
                futures.add(executor.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                    new TransactionTemplate(manager).executeWithoutResult(tx -> {
                        locks.lock(ids);
                        finished.incrementAndGet();
                    });
                }));
            start.countDown();
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
            assertThat(finished.get()).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM care_resident_lock WHERE resident_id IN (99101,99102)",
                            Integer.class))
                    .isEqualTo(2);
        } finally {
            jdbc.update("DELETE FROM care_resident_lock WHERE resident_id IN (99101,99102)");
        }
    }

    @Test
    void NFR_04_dailyMilestoneRollsBackWithFailedWorkAndRetriesExactlyOnce() {
        var calls = new AtomicInteger();
        CareDailyWork work = new CareDailyWork() {
            public String name() {
                return "TEST_RETRY";
            }

            public List<LocalDate> dueDates(LocalDate today) {
                return List.of(today);
            }

            public int run(LocalDate date, Instant now) {
                if (calls.incrementAndGet() == 1) throw new IllegalStateException("TEST");
                return 1;
            }
        };
        var day = LocalDate.of(2026, 10, 27);
        try {
            assertThatThrownBy(() -> runner.run(work, day, at)).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM m04_job_milestone WHERE job='TEST_RETRY'", Integer.class))
                    .isZero();
            assertThat(runner.run(work, day, at)).isEqualTo(1);
            assertThat(runner.run(work, day, at.plusSeconds(60))).isZero();
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            jdbc.update("DELETE FROM m04_job_milestone WHERE job='TEST_RETRY'");
        }
    }

    @Test
    void FR_052_jobsUseInjectedClockAndDailyWorkCanCatchUpInDateOrder() {
        var time = new BusinessTime(Clock.fixed(at, BusinessTime.ZONE));
        var seen = new ArrayList<Instant>();
        var days = new ArrayList<LocalDate>();
        CareMinuteWork minute = now -> {
            seen.add(now);
            return 0;
        };
        CareDailyWork daily = new CareDailyWork() {
            public String name() {
                return "TEST_CATCHUP";
            }

            public List<LocalDate> dueDates(LocalDate today) {
                return List.of(today, today.minusDays(1), today);
            }

            public int run(LocalDate date, Instant now) {
                days.add(date);
                assertThat(now).isEqualTo(at);
                return 0;
            }
        };
        try {
            new CareMinuteJob(List.of(minute), runs, time, new MockEnvironment()).runNow();
            var job = new CareDailyJob(List.of(daily), runner, runs, time, new MockEnvironment());
            job.runNow();
            job.runNow();
            assertThat(seen).containsExactly(at);
            assertThat(days).containsExactly(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 27));
        } finally {
            jdbc.update("DELETE FROM m04_job_milestone WHERE job='TEST_CATCHUP'");
        }
    }
}
