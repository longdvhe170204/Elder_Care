package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.*;
import com.cambae.backend.m09.api.events.ShiftBelowStaffingThreshold;
import com.cambae.backend.m09.ratio.internal.domain.StaffingRatioCalculator.Result;
import com.cambae.backend.m09.ratio.internal.service.StaffingRatioService;
import com.cambae.backend.support.*;
import com.cambae.backend.support.fake.m03.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;

@Import(FakeM03Config.class)
@RecordApplicationEvents
class StaffingRatioWarningIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StaffingRatioService service;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @Autowired
    FakeLocations locations;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    long shift;
    long floor;
    Instant start;

    @BeforeEach
    void setup() {
        clock.setTo(Instant.parse("2026-10-25T00:00:00Z"));
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        start = time.now().plus(Duration.ofDays(3));
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu tỷ lệ]','07:00','15:00','FLOOR',FALSE)");
        long schedule = AssignmentFixtures.insert(
                jdbc, "INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2026-10','FLOOR',?)", floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover) VALUES (?,?,?,?,'FLOOR',?,FALSE)",
                schedule,
                template,
                sql(start),
                sql(start.plus(Duration.ofHours(8))),
                floor);
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    private LocalDateTime sql(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private Result result(boolean passed) {
        return new Result(
                new BigDecimal("35.00"),
                passed ? 5 : 4,
                new BigDecimal(passed ? "7.00" : "8.75"),
                new BigDecimal("8.00"),
                passed);
    }

    private long count() {
        return events.stream(ShiftBelowStaffingThreshold.class)
                .filter(e -> e.shiftId() == shift)
                .count();
    }

    @Test
    void FR_036_draftNoNotification_publishWarnsOnce_recoveryAllowsNewEpisode() {
        service.record(shift, "DRAFT_SAVED", result(false));
        assertThat(count()).isZero();
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        service.record(shift, "PUBLISHED", result(false));
        service.record(shift, "CARE_LEVEL_CHANGED", result(false));
        assertThat(count()).isEqualTo(1);
        var first = events.stream(ShiftBelowStaffingThreshold.class)
                .filter(e -> e.shiftId() == shift)
                .findFirst()
                .orElseThrow();
        assertThat(first.notificationRequests().getFirst().recipientGroups())
                .anyMatch(RecipientGroup.FloorManager.class::isInstance);
        assertThat(first.notificationRequests().getFirst().recipientGroups())
                .noneMatch(RecipientGroup.FacilityRole.class::isInstance);
        service.record(shift, "STAFF_ADDED", result(true));
        clock.advanceBy(Duration.ofSeconds(1));
        service.record(shift, "STAFF_ABSENT", result(false));
        assertThat(count()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staffing_ratio_calculation WHERE shift_id=?", Integer.class, shift))
                .isEqualTo(5);
    }

    @Test
    void FR_036_hourlyNearStartManagerOnlyOnce_andNoDuplicateFloorWarning() {
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        service.record(shift, "PUBLISHED", result(false));
        clock.setTo(start.minus(Duration.ofHours(1)));
        service.run(time.now(), time.now());
        service.run(time.now(), time.now());
        var notices = events.stream(ShiftBelowStaffingThreshold.class)
                .filter(e -> e.shiftId() == shift)
                .toList();
        assertThat(notices).hasSize(2);
        assertThat(notices.getLast().notificationRequests().getFirst().recipientGroups())
                .allMatch(RecipientGroup.FacilityRole.class::isInstance);
        assertThat(jdbc.queryForObject(
                        "SELECT manager_notified_below_threshold_at IS NOT NULL FROM work_shift WHERE id=?",
                        Boolean.class,
                        shift))
                .isTrue();
    }

    @Test
    void FR_037_closedShiftNotRewritten() {
        jdbc.update("UPDATE work_shift SET status='CLOSED' WHERE id=?", shift);
        service.record(shift, "CARE_LEVEL_CHANGED", result(false));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staffing_ratio_calculation WHERE shift_id=?", Integer.class, shift))
                .isZero();
        assertThat(count()).isZero();
    }

    @Test
    void FR_037_failedCommandRollsBackSnapshotAndWarningFlag() {
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
            service.record(shift, "PUBLISHED", result(false));
            tx.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staffing_ratio_calculation WHERE shift_id=?", Integer.class, shift))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT below_threshold FROM work_shift WHERE id=?", Boolean.class, shift))
                .isFalse();
    }

    @Test
    void FR_036_concurrentTransactionsWithOldReadSnapshotsStillNotifyOnlyOnce() throws Exception {
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Void> calculate = () -> {
                new org.springframework.transaction.support.TransactionTemplate(transactions)
                        .executeWithoutResult(tx -> {
                            jdbc.queryForObject(
                                    "SELECT below_threshold FROM work_shift WHERE id=?", Boolean.class, shift);
                            try {
                                barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            service.record(shift, "CARE_LEVEL_CHANGED", result(false));
                        });
                return null;
            };
            var a = executor.submit(calculate);
            var b = executor.submit(calculate);
            a.get(15, java.util.concurrent.TimeUnit.SECONDS);
            b.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE=? AND JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.shiftId'))=?",
                        Integer.class,
                        ShiftBelowStaffingThreshold.class.getName(),
                        Long.toString(shift)))
                .isEqualTo(1);
    }
}
