package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import com.eldercare.backend.staffing.api.events.ShiftStarted;
import com.eldercare.backend.staffing.internal.shift.service.ShiftHandoverState;
import com.eldercare.backend.staffing.scheduling.M09MinuteJob;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** T014: chạy qua job thật để nhật ký mang người thực hiện Bộ lập lịch, kiểm chạy bù và chống lặp. */
@Import(ShiftLifecycleIT.Handovers.class)
@RecordApplicationEvents
class ShiftLifecycleIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestClockControl clock;

    @Autowired
    M09MinuteJob job;

    @Autowired
    ApplicationEvents events;

    @Autowired
    Prepared handovers;

    @AfterEach
    void reset() {
        clock.reset();
        handovers.ids.clear();
    }

    private long fixture(boolean handover, String day) {
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Vòng đời ca] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FACILITY' AS scope_type,? AS requires_handover) src",
                handover);
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,status) VALUES (?,'FACILITY','PUBLISHED')",
                day.substring(0, 7));
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,requires_handover,status) VALUES (?,?,CONCAT(?,' 00:00'),CONCAT(?,' 08:00'),'FACILITY',?,'PUBLISHED')",
                schedule,
                template,
                day,
                day,
                handover);
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM work_shift WHERE id=?", String.class, id);
    }

    @Test
    void FR_022_startAndEndCatchUpOnceAndNeverCreateBusinessRecords() {
        long id = fixture(false, "2033-01-01");
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM work_shift", Integer.class);
        clock.setTo(Instant.parse("2033-01-01T10:00:00Z"));
        job.runNow();
        job.runNow();
        assertThat(status(id)).isEqualTo("CLOSED");
        assertThat(events.stream(ShiftStarted.class)
                        .filter(e -> e.shiftId() == id)
                        .count())
                .isOne();
        assertThat(events.stream(ShiftEnded.class)
                        .filter(e -> e.shiftId() == id)
                        .count())
                .isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM work_shift", Integer.class))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.SHIFT' AND entity_id=? AND actor_type='SCHEDULER'",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(2);
        var runIds = jdbc.queryForList(
                "SELECT JSON_UNQUOTE(basis->'$.schedulerRunId') FROM audit_log WHERE entity_type='M09.SHIFT' AND entity_id=? AND actor_type='SCHEDULER' ORDER BY id",
                String.class,
                Long.toString(id));
        assertThat(runIds).as("FR_022_basisSchedulerRunId").hasSize(2).doesNotContainNull();
        assertThat(runIds)
                .allSatisfy(value ->
                        assertThat(java.util.UUID.fromString(value).toString()).isEqualTo(value));
        assertThat(runIds).containsOnly(runIds.getFirst());
    }

    @Test
    void BR_M09_07_missingOrIncompleteHandoverKeepsShiftAwaitingThenClosesWhenPrepared() {
        long id = fixture(true, "2033-02-01");
        clock.setTo(Instant.parse("2033-02-01T10:00:00Z"));
        job.runNow();
        assertThat(status(id)).isEqualTo("AWAITING_HANDOVER");
        handovers.ids.add(id);
        clock.setTo(Instant.parse("2033-02-01T10:01:00Z"));
        job.runNow();
        assertThat(status(id)).isEqualTo("CLOSED");
        assertThat(events.stream(ShiftEnded.class)
                        .filter(e -> e.shiftId() == id)
                        .count())
                .isOne();
    }

    static class Prepared implements ShiftHandoverState {
        final Set<Long> ids = new HashSet<>();

        public boolean prepared(long shiftId) {
            return ids.contains(shiftId);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Handovers {
        @Bean
        Prepared prepared() {
            return new Prepared();
        }
    }
}
