package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
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
