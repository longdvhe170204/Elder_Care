package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.support.JobLogCapture;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.event.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** T057/T059: job qua đồng hồ giả, không tạo phiếu hoặc phân bổ. */
@RecordApplicationEvents
@Transactional
class FR_044_BedRemindersIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    ApplicationEvents events;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void reset() {
        clock.reset();
    }

    private void run(String bean) {
        ReflectionTestUtils.invokeMethod(context.getBean(bean), "runNow");
    }

    @Test
    void FR_044_overdueBedRemindsOnceAndReentryAllowsAnotherReminder() {
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now()),
                Timestamp.from(time.now().plusSeconds(3600)),
                bed);
        run("bedReadinessReminderJob");
        assertThat(events.stream(BedReadinessReminder.class)
                        .filter(e -> e.bedId() == bed)
                        .count())
                .isZero();
        clock.advanceBy(Duration.ofHours(2));
        try (var log = new JobLogCapture()) {
            run("bedReadinessReminderJob");
            assertSchedulerRun("M03.BED", bed, "REMIND_BED_READINESS", log);
        }
        run("bedReadinessReminderJob");
        assertThat(events.stream(BedReadinessReminder.class)
                        .filter(e -> e.bedId() == bed)
                        .count())
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ready_reminded_at IS NOT NULL FROM bed WHERE id=?", Boolean.class, bed))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
        jdbc.update(
                "UPDATE bed SET ready_reminded_at=NULL,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(1)),
                bed);
        run("bedReadinessReminderJob");
        assertThat(events.stream(BedReadinessReminder.class)
                        .filter(e -> e.bedId() == bed)
                        .count())
                .isEqualTo(2);
    }

    @Test
    void FR_025a_periodicWithoutBedReminderStopsWhenFlagClears() {
        long resident = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident_position(resident_id,without_bed_since) VALUES(?,?)",
                resident,
                Timestamp.from(time.now()));
        jdbc.update(
                "INSERT INTO allocation_reminder VALUES('RESIDENT',?,'WITHOUT_BED',?,NULL,0)",
                resident,
                Timestamp.from(time.now()));
        run("allocationReminderJob");
        assertThat(events.stream(AllocationReminder.class)
                        .filter(e -> e.targetId() == resident)
                        .count())
                .isZero();
        clock.advanceBy(Duration.ofDays(2));
        try (var log = new JobLogCapture()) {
            run("allocationReminderJob");
            assertSchedulerRun("M03.RESIDENT", resident, "REMIND_ALLOCATIONS", log);
        }
        run("allocationReminderJob");
        assertThat(events.stream(AllocationReminder.class)
                        .filter(e -> e.targetId() == resident)
                        .count())
                .isEqualTo(1);
        jdbc.update("UPDATE resident_position SET without_bed_since=NULL WHERE resident_id=?", resident);
        clock.advanceBy(Duration.ofDays(2));
        run("allocationReminderJob");
        assertThat(events.stream(AllocationReminder.class)
                        .filter(e -> e.targetId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_type='RESIDENT' AND target_id=?",
                        Integer.class,
                        resident))
                .isZero();
    }

    @Test
    void FR_030_034_eachAllocationFlagUsesItsOwnCounterAndRecipientGroup() {
        long resident = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source,awaiting_request,awaiting_same_price,room_unsuitable) VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT',TRUE,TRUE,TRUE)",
                resident,
                bed,
                Timestamp.from(time.now()));
        long allocation =
                jdbc.queryForObject("SELECT id FROM bed_allocation WHERE resident_id=?", Long.class, resident);
        run("allocationReminderJob");
        clock.advanceBy(Duration.ofDays(2));
        run("allocationReminderJob");
        run("allocationReminderJob");
        var reminders = events.stream(AllocationReminder.class)
                .filter(e -> e.targetId() == allocation)
                .toList();
        assertThat(reminders)
                .extracting(AllocationReminder::flagType)
                .containsExactlyInAnyOrder("AWAITING_REQUEST", "AWAITING_SAME_PRICE", "ROOM_UNSUITABLE");
        assertThat(reminders).allSatisfy(e -> {
            assertThat(e.reminderNo()).isEqualTo(1);
            assertThat(e.notification().recipients().stream()
                            .anyMatch(
                                    com.eldercare.backend.common.notification.RecipientGroup.FloorManager.class
                                            ::isInstance))
                    .isEqualTo(!e.flagType().equals("AWAITING_REQUEST"));
        });
        jdbc.update(
                "UPDATE bed_allocation SET awaiting_request=FALSE,awaiting_same_price=FALSE,room_unsuitable=FALSE WHERE id=?",
                allocation);
        clock.advanceBy(Duration.ofDays(2));
        run("allocationReminderJob");
        assertThat(events.stream(AllocationReminder.class)
                        .filter(e -> e.targetId() == allocation)
                        .count())
                .isEqualTo(3);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=?",
                        Integer.class,
                        allocation))
                .isZero();
    }

    @Autowired
    com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore commands;

    /** FR-045: căn cứ nhật ký phải tra được đúng lần chạy UUID trong jobs.log. */
    private void assertSchedulerRun(String entityType, long id, String job, JobLogCapture log) {
        String runId = jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(basis,'$.schedulerRunId')) FROM audit_log"
                        + " WHERE action='M03_REMINDER' AND entity_type=? AND entity_id=? ORDER BY id DESC LIMIT 1",
                String.class,
                entityType,
                Long.toString(id));
        assertThat(runId).isNotNull();
        assertThat(UUID.fromString(runId).toString()).isEqualTo(runId);
        assertThat(log.events(runId)).hasSize(2);
        assertThat(log.events(runId).getFirst()).containsEntry("job", job).containsEntry("event", "START");
        assertThat(log.events(runId).getLast()).containsEntry("event", "FINISH").containsEntry("result", "SUCCEEDED");
    }

    @Test
    void FR_034_flagCommandStartsAndResetsReminderCycleBeforeFirstJob() {
        long resident = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT')",
                resident,
                bed,
                Timestamp.from(time.now()));
        long allocation =
                jdbc.queryForObject("SELECT id FROM bed_allocation WHERE resident_id=?", Long.class, resident);
        commands.markUnsuitable(allocation, true);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=? AND flag_type='ROOM_UNSUITABLE'",
                        Integer.class,
                        allocation))
                .isEqualTo(1);
        clock.advanceBy(Duration.ofDays(2));
        run("allocationReminderJob");
        assertThat(events.stream(AllocationReminder.class)
                        .filter(e -> e.targetId() == allocation)
                        .count())
                .isEqualTo(1);
        commands.markUnsuitable(allocation, false);
        commands.markUnsuitable(allocation, true);
        run("allocationReminderJob");
        assertThat(events.stream(AllocationReminder.class)
                        .filter(e -> e.targetId() == allocation)
                        .count())
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT reminder_count FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=? AND flag_type='ROOM_UNSUITABLE'",
                        Integer.class,
                        allocation))
                .isZero();
    }
}
