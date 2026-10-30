package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import com.eldercare.backend.staffing.api.events.ShiftStarted;
import com.eldercare.backend.staffing.scheduling.M09MinuteJob;
import com.eldercare.backend.staffing.shift.internal.service.port.ShiftHandoverState;
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
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Vòng đời ca]','07:00','15:00','FACILITY',?)",
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
