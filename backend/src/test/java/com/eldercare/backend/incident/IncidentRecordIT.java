package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.NewIncident;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Bảo vệ nội dung ghi nhận trong bảng sự cố đã gộp dữ liệu xử lý theo R-15. */
@Transactional
class IncidentRecordIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    IncidentStore incidents;

    @Test
    void FR_040_incidentContentCannotBeUpdatedOrDeleted() {
        long id = insert();
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET description = 'Đổi nội dung' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM incident WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT description FROM incident WHERE id = ?", String.class, id))
                .isEqualTo("Ghi nhận ban đầu");
    }

    @Test
    void FR_045_incidentHandlingCanChangeUntilClosed() {
        long id = insert();
        assertThat(jdbc.update("UPDATE incident SET status = 'IN_PROGRESS', version = version + 1 WHERE id = ?", id))
                .isEqualTo(1);
        jdbc.update("UPDATE incident SET status = 'CLOSED', version = version + 1 WHERE id = ?", id);
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET status = 'NEW' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M05_22_levelChangeKeepsEmergencyHistoryAndVersion() {
        long id = insert();
        jdbc.update("UPDATE incident SET level = 'CRITICAL', version = version + 1 WHERE id = ?", id);
        var before = incidents.lock(id).orElseThrow();

        incidents.changeLevel(id, IncidentLevel.MEDIUM, true, before.version());

        var after = incidents.lock(id).orElseThrow();
        assertThat(after.level()).isEqualTo(IncidentLevel.MEDIUM);
        assertThat(after.wasEmergency()).isTrue();
        assertThat(after.version()).isEqualTo(before.version() + 1);
    }

    @Test
    void FR_045_receiveSetsReceiverAssigneeAndStopsEscalationOnlyOnce() {
        long id = insert();
        var before = incidents.lock(id).orElseThrow();
        long nurse = TestUsers.nextId();
        var at = Instant.parse("2026-11-04T01:00:00Z");

        incidents.receive(id, nurse, at, before.version());

        var row = jdbc.queryForMap(
                "SELECT status, received_by, assignee_id, escalation_stopped, version FROM incident WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(((Number) row.get("received_by")).longValue()).isEqualTo(nurse);
        assertThat(((Number) row.get("assignee_id")).longValue()).isEqualTo(nurse);
        assertThat(row.get("escalation_stopped")).isEqualTo(true);
        assertThat(((Number) row.get("version")).intValue()).isEqualTo(before.version() + 1);
        assertThatThrownBy(() -> incidents.receive(id, TestUsers.nextId(), at, before.version() + 1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("đồng thời");
    }

    @Test
    void FR_041_insertedCriticalIncidentIsOpenEmergencyUntilClosed() {
        long resident = TestUsers.nextId();
        var at = Instant.parse("2026-11-04T01:00:00Z");
        long id = incidents.insert(new NewIncident(
                resident,
                TestUsers.nextId(),
                "FALL",
                "CARE",
                null,
                at,
                7L,
                "Phòng 101",
                null,
                "Ngã trong phòng",
                null,
                IncidentLevel.CRITICAL,
                null,
                true,
                TestUsers.nextId(),
                at,
                null,
                true));

        assertThat(incidents.openEmergency(resident)).contains(id);
        var row = jdbc.queryForMap(
                "SELECT status, level, initial_level, was_emergency, out_of_scope, no_on_duty_doctor,"
                        + " current_type_id = incident_type_id AS same_type FROM incident WHERE id = ?",
                id);
        assertThat(row.get("status")).isEqualTo("NEW");
        assertThat(row.get("initial_level")).isEqualTo("CRITICAL");
        assertThat(row.get("was_emergency")).isEqualTo(true);
        assertThat(row.get("out_of_scope")).isEqualTo(true);
        assertThat(row.get("no_on_duty_doctor")).isEqualTo(true);
        assertThat(((Number) row.get("same_type")).intValue()).isEqualTo(1);

        jdbc.update("UPDATE incident SET status = 'CLOSED', version = version + 1 WHERE id = ?", id);
        assertThat(incidents.openEmergency(resident)).isEmpty();
    }

    @Test
    void BR_M05_09_closeStoresResultAndStopsEscalationThenIncidentIsFinal() {
        long id = insert();
        var start = incidents.lock(id).orElseThrow();
        incidents.changeStatus(id, "NEW", "IN_PROGRESS", start.version());
        incidents.changeStatus(id, "IN_PROGRESS", "MONITORING", start.version() + 1);
        var at = Instant.parse("2026-11-04T04:00:00Z");
        long closer = TestUsers.nextId();

        incidents.close(id, "Đã ổn định", "RULED_OUT", closer, at, start.version() + 2);

        var row = jdbc.queryForMap(
                "SELECT status, resolution, infection_result, closed_by, escalation_stopped FROM incident WHERE id = ?",
                id);
        assertThat(row.get("status")).isEqualTo("CLOSED");
        assertThat(row.get("resolution")).isEqualTo("Đã ổn định");
        assertThat(row.get("infection_result")).isEqualTo("RULED_OUT");
        assertThat(((Number) row.get("closed_by")).longValue()).isEqualTo(closer);
        assertThat(row.get("escalation_stopped")).isEqualTo(true);
        assertThat(incidents.currentTypeCode(id)).isEqualTo("SUSPECTED_INFECTION");
        assertThatThrownBy(() -> incidents.changeStatus(id, "CLOSED", "IN_PROGRESS", start.version() + 3))
                .isInstanceOf(DataAccessException.class);
    }

    private long insert() {
        long id = TestUsers.nextId();
        var at = Timestamp.from(Instant.parse("2026-11-03T00:00:00Z"));
        jdbc.update(
                """
                INSERT INTO incident (id, resident_id, incident_type_id, current_type_id,
                  type_code, discovered_at, description, initial_level, level,
                  recorded_by, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 'SUSPECTED_INFECTION', ?, 'Ghi nhận ban đầu', 'MEDIUM', 'MEDIUM', ?, ?, ?)
                """, id, TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), at, TestUsers.nextId(), at, at);
        return id;
    }
}
