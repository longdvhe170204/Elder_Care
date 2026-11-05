package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.incident.api.events.IncidentEscalated;
import com.eldercare.backend.incident.api.events.IncidentMaximumEscalationReminder;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentEscalationService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-044 (research S-11 "Leo thang của sự cố"): sự cố quá hạn tiếp nhận leo thang theo chuỗi của cảnh báo (FR-033,
 * FR-034) nhưng giữ trạng thái Mới, chỉ tăng cấp; mỗi lần chạy một cấp (BR-M05-25); đã tiếp nhận thì dừng.
 */
class FR_044_IncidentEscalationIT extends MySqlIntegrationTestBase {
    private static final Instant NOW = Instant.parse("2030-03-05T03:00:00Z");
    private static final Duration INTERVAL = Duration.ofMinutes(15);

    @Autowired
    IncidentStore incidents;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    private final ParameterApi parameters = mock(ParameterApi.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final List<Object> published = new ArrayList<>();
    private IncidentEscalationService service;

    @BeforeEach
    void setUp() {
        var value = mock(ParameterValue.class);
        when(value.asDuration()).thenReturn(INTERVAL);
        when(parameters.read(ParameterCode.CFG_M05_01)).thenReturn(value);
        when(staff.assignedFloorManager(any(Long.class), any())).thenReturn(Optional.of(TestUsers.nextId()));
        when(staff.onDutyDoctors(any())).thenReturn(List.of());
        service = new IncidentEscalationService(incidents, published::add, parameters, transaction, staff);
    }

    @Test
    void FR_044_overdueIncidentKeepsNewAndGoesUpOneLevel() {
        long id = insert("NEW", 0, NOW.minus(Duration.ofHours(3)));

        assertThat(service.escalate(id, NOW)).isTrue();

        var row = row(id);
        assertThat(row.get("status")).isEqualTo("NEW");
        assertThat(row.get("escalation_level")).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                                "SELECT acknowledgement_deadline FROM incident WHERE id = ?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(NOW.plus(INTERVAL));
        assertThat(published).singleElement().isInstanceOfSatisfying(IncidentEscalated.class, event -> {
            assertThat(event.incidentId()).isEqualTo(id);
            assertThat(event.escalationLevel()).isEqualTo(1);
            assertThat(event.notificationRequests()).singleElement().satisfies(request -> {
                assertThat(request.sourceRecord().type()).isEqualTo("M05.INCIDENT");
                assertThat(request.eventKey()).endsWith(":ESCALATED:1");
            });
        });
        assertThat(service.escalate(id, NOW))
                .as("hạn mới ở tương lai: chạy lại không lên tiếp")
                .isFalse();
    }

    @Test
    void FR_044_levelTwoOverdueMarksMaximumThenRemindsOncePerInterval() {
        long id = insert("NEW", 2, NOW.minusSeconds(60));

        service.escalate(id, NOW);
        assertThat(row(id).get("max_escalation_at")).isNotNull();
        assertThat(published).isEmpty();

        Instant later = NOW.plus(INTERVAL);
        service.escalate(id, later);
        service.escalate(id, later);
        assertThat(row(id).get("max_reminder_count")).isEqualTo(1);
        assertThat(published)
                .singleElement()
                .isInstanceOfSatisfying(
                        IncidentMaximumEscalationReminder.class,
                        event -> assertThat(event.eventKey()).endsWith(":MAX_REMINDER:1"));
    }

    @Test
    void FR_044_receivedIncidentIsNotEscalated() {
        long id = insert("IN_PROGRESS", 0, NOW.minusSeconds(60));
        jdbc.update("UPDATE incident SET escalation_stopped = TRUE WHERE id = ?", id);

        assertThat(service.escalate(id, NOW)).isFalse();
        assertThat(service.run(NOW)).isGreaterThanOrEqualTo(0);
        assertThat(row(id).get("escalation_level")).isEqualTo(0);
    }

    private long insert(String status, int level, Instant deadline) {
        long id = TestUsers.nextId();
        long type = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type = 'M05.INCIDENT_TYPE' AND code = 'FALL'", Long.class);
        var at = Timestamp.from(NOW.minus(Duration.ofDays(1)));
        jdbc.update(
                """
                INSERT INTO incident (id, resident_id, incident_type_id, current_type_id, type_code, discovered_at,
                  floor_area_id, description, initial_level, level, was_emergency, recorded_by, created_at, confirmed_at,
                  escalation_level, acknowledgement_deadline)
                VALUES (?, ?, ?, ?, 'FALL', ?, ?, 'Mô tả', 'MEDIUM', 'MEDIUM', FALSE, ?, ?, ?, ?, ?)
                """,
                id,
                TestUsers.nextId(),
                type,
                type,
                at,
                TestUsers.nextId(),
                TestUsers.nextId(),
                at,
                at,
                level,
                Timestamp.from(deadline));
        if (!"NEW".equals(status)) jdbc.update("UPDATE incident SET status = ? WHERE id = ?", status, id);
        return id;
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap(
                "SELECT status, escalation_level, acknowledgement_deadline, max_escalation_at, max_reminder_count"
                        + " FROM incident WHERE id = ?",
                id);
    }
}
