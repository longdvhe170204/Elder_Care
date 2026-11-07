package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.api.events.IncidentAssigned;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Giao người xử lý sự cố qua HTTP với quyền, phạm vi 002 thật và phân công giả của 008 (spec 007 US4 #11; FR-045a,
 * Q-339, Q-71; T066).
 */
@RecordApplicationEvents
class FR_045a_AssignIncidentHandlerIT extends IncidentHttpTestBase {
    @Autowired
    ApplicationEvents events;

    @Test
    void FR_045a_floorManagerAssignsNewIncidentToNurseInScope() throws Exception {
        long incident = incident("FALL", "MEDIUM", "NEW", null, TestUsers.nextId());

        assignHandler(floorManager, incident, nurse.accountId(), "Theo dõi ngay")
                .andExpect(status().isOk());

        var row = jdbc.queryForMap(
                "SELECT status, assignee_id, received_by, escalation_stopped FROM incident WHERE id = ?", incident);
        assertThat(row.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(((Number) row.get("assignee_id")).longValue()).isEqualTo(nurse.accountId());
        assertThat(((Number) row.get("received_by")).longValue()).isEqualTo(floorManager.accountId());
        assertThat(row.get("escalation_stopped")).isEqualTo(true);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.INCIDENT_ASSIGNMENT' AND entity_id = ?",
                        Integer.class,
                        incident))
                .isEqualTo(1);
        assertThat(events.stream(IncidentAssigned.class)).singleElement().satisfies(event -> {
            assertThat(event.incidentId()).isEqualTo(incident);
            assertThat(event.level()).isEqualTo("MEDIUM");
        });
    }

    private ResultActions assignHandler(TestUser as, long incident, long assignee, String note) throws Exception {
        return post(
                as,
                "/api/v1/incidents/{id}/assign-handler",
                incident,
                "{\"assigneeId\":" + assignee + ",\"note\":\"" + note + "\"}");
    }

    private long facilityIncident() {
        long id = TestUsers.nextId();
        long type = typeId("FACILITY");
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO incident (id, incident_type_id, current_type_id, type_code, discovered_at, floor_area_id,
                  description, initial_level, level, recorded_by, created_at, confirmed_at)
                VALUES (?, ?, ?, 'FACILITY', ?, ?, 'Vỡ ống nước', 'LOW', 'LOW', ?, ?, ?)
                """, id, type, type, at, floor, TestUsers.nextId(), at, at);
        return id;
    }
}
