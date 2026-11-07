package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tiếp nhận cảnh báo trên MySQL: một người thắng, người sau bị từ chối (FR-029). */
@Transactional
class AlertReceiveStoreIT extends MySqlIntegrationTestBase {
    @Autowired
    AlertStore alerts;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_029_receiveStoresReceiverAndLevelAndStopsEscalationOnce() {
        long resident = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO alert (resident_id, type, type_lock, level, first_at, latest_at, status, escalation_level, version)
                VALUES (?, 'CHI_SO', 'MONITORING_ALERT:SPO2', 'MEDIUM', NOW(6), NOW(6), 'ESCALATED', 1, 0)
                """, resident);
        long id = jdbc.queryForObject("SELECT id FROM alert WHERE resident_id = ?", Long.class, resident);
        var before = alerts.lockForReceive(id).orElseThrow();
        assertThat(before.status()).isEqualTo(AlertStatus.ESCALATED);
        assertThat(before.escalationLevel()).isEqualTo(1);
        long nurse = TestUsers.nextId();
        var at = Instant.parse("2026-11-04T03:00:00Z");

        alerts.receive(id, nurse, at, IncidentLevel.MEDIUM, before.version());

        var after = alerts.lockForReceive(id).orElseThrow();
        assertThat(after.status()).isEqualTo(AlertStatus.RECEIVED);
        assertThat(after.receiverId()).isEqualTo(nurse);
        assertThat(after.receivedAt()).isEqualTo(at);
        var row = jdbc.queryForMap(
                "SELECT assignee_id, level_at_received, escalation_stopped FROM alert WHERE id = ?", id);
        assertThat(((Number) row.get("assignee_id")).longValue()).isEqualTo(nurse);
        assertThat(row.get("level_at_received")).isEqualTo("MEDIUM");
        assertThat(row.get("escalation_stopped")).isEqualTo(true);
        assertThatThrownBy(() -> alerts.receive(id, TestUsers.nextId(), at, IncidentLevel.MEDIUM, after.version()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("đồng thời");
    }
}
