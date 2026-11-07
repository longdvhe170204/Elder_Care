package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi;
import com.eldercare.backend.care.api.events.ActivityRegistrationsCancelled;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.care.FakeActivityHealthGate;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T010 (FR-021, FR-021a, FR-022; US2 kịch bản khoanh vùng): 007 gọi {@code ActivityRegistrationAutoCancelApi} trong
 * lệnh khoanh vùng, gắn dấu nghi nhiễm; trạng thái cuối qua bean tham gia chuyển trạng thái của 001.
 */
@Transactional
@RecordApplicationEvents
@Import(FakeM04Config.class)
class FR_021_KhoanhVungIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ActivityRegistrationAutoCancelApi autoCancel;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    FakeActivityHealthGate gate;

    @Autowired
    ApplicationEvents events;

    long activity(boolean group) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by) VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?,NOW(6),1)",
                id,
                "{\"name\":\"Hát\",\"typeId\":1,\"scope\":\"FLOOR\",\"floorAreaId\":7,\"maxParticipants\":20,\"charged\":false,"
                        + "\"groupActivity\":" + group + ",\"createdByStaffId\":1}");
        return id;
    }

    long session(long activity, Long room, int daysAhead) {
        long id = TestUsers.nextId();
        var day = time.today().plusDays(daysAhead);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,location_room_id,floor_area_id,status,created_at,created_by)"
                        + " VALUES(?,?,?,?,'SINGLE',?,7,'SCHEDULED',NOW(6),1)",
                id,
                activity,
                day.atTime(9, 0),
                day.atTime(10, 0),
                room);
        return id;
    }

    long registration(long session, long resident, String source) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_registration(id,session_id,resident_id,source,registered_by_staff_id,registered_at,status)"
                        + " VALUES(?,?,?,?,?,NOW(6),'REGISTERED')",
                id,
                session,
                resident,
                source,
                TestUsers.nextId());
        return id;
    }

    String status(String table, long id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id);
    }

    String autoCancel(long registration) {
        return jdbc.queryForObject(
                "SELECT auto_cancel FROM activity_registration WHERE id=?", String.class, registration);
    }

    @Test
    void FR_021_isolationCancelsSessionsInZoneAndRegistrationsOfIsolatedResidents() {
        long room = TestUsers.nextId(), isolated = TestUsers.nextId(), other = TestUsers.nextId();
        long a = activity(true);
        long inZone = session(a, room, 1);
        long elsewhere = session(a, TestUsers.nextId(), 2);
        long r1 = registration(inZone, other, "MANUAL");
        long r2 = registration(elsewhere, isolated, "MANUAL");
        long r3 = registration(elsewhere, other, "MANUAL");
        gate.isolateRoom(room);
        gate.isolateResident(isolated);

        autoCancel.cancelForIsolation(TestUsers.nextId(), time.now());

        assertThat(status("activity_session", inZone)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancelled_by FROM activity_session WHERE id=?", String.class, inZone))
                .isEqualTo("ISOLATION");
        assertThat(status("activity_registration", r1)).isEqualTo("CANCELLED");
        assertThat(status("activity_session", elsewhere)).isEqualTo("SCHEDULED");
        assertThat(autoCancel(r2)).isEqualTo("ISOLATION");
        assertThat(status("activity_registration", r3)).isEqualTo("REGISTERED");
        assertThat(events.stream(ActivityRegistrationsCancelled.class).map(ActivityRegistrationsCancelled::sessionId))
                .contains(inZone, elsewhere);
    }

    @Test
    void FR_021a_suspectedInfectionCancelsOnlyGroupActivityRegistrations() {
        long resident = TestUsers.nextId();
        long group = session(activity(true), null, 1);
        long individual = session(activity(false), null, 2);
        long g = registration(group, resident, "MANUAL");
        long i = registration(individual, resident, "MANUAL");

        autoCancel.cancelForSuspectedInfection(resident, time.now());

        assertThat(autoCancel(g)).isEqualTo("INFECTION_SUSPECTED");
        assertThat(status("activity_registration", i)).isEqualTo("REGISTERED");
    }

    @Test
    void FR_022_finalStatusCancelsFutureRegistrationsAndEndsRegularParticipation() {
        long resident = TestUsers.nextId();
        long a = activity(true);
        long s = session(a, null, 1);
        long r = registration(s, resident, "REGULAR");
        long regular = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,resident_id,entity_type,entity_id,status,content,unique_key,created_at,created_by)"
                        + " VALUES(?,'M04.REGULAR_PARTICIPANT','M04',?,'M04.ACTIVITY',?,'ACTIVE',?,?,NOW(6),1)",
                regular,
                resident,
                a,
                "{\"activityId\":" + a + "}",
                a + ":" + resident);
        var participant = participants.stream()
                .filter(p -> p.getClass().getPackageName().startsWith("com.eldercare.backend.care.internal.session"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu bean tham gia chuyển trạng thái của 014"));
        assertThat(participant.interestedCommands())
                .contains(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);

        participant.applyEffects(new TransitionResult(
                resident,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.RESIDENCY_ENDED,
                StatusCommand.END_RESIDENCY,
                0,
                time.now(),
                null));

        assertThat(autoCancel(r)).isEqualTo("FINAL_STATUS");
        assertThat(jdbc.queryForObject("SELECT status FROM simple_record WHERE id=?", String.class, regular))
                .isEqualTo("ENDED");
        assertThat(jdbc.queryForObject("SELECT unique_key FROM simple_record WHERE id=?", String.class, regular))
                .isNull();
    }
}
