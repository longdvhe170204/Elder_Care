package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.incident.api.events.AlertEscalated;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertEscalationService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Leo thang theo phút trên MySQL (BR-M05-01, BR-M05-25, FR-033, FR-034, FR-084 a, d): một cấp mỗi lần chạy, bỏ qua cấp
 * không có người, nhắc tối đa không trùng khi chạy lại.
 */
class BR_M05_01_EscalationIT extends MySqlIntegrationTestBase {
    private static final Instant NOW = Instant.parse("2030-03-04T03:00:00Z");
    private static final Duration INTERVAL = Duration.ofMinutes(15);

    @Autowired
    AlertStore alerts;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    private final ParameterApi parameters = mock(ParameterApi.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final List<Object> published = new ArrayList<>();
    private AlertEscalationService service;

    @BeforeEach
    void setUp() {
        var value = mock(ParameterValue.class);
        when(value.asDuration()).thenReturn(INTERVAL);
        when(parameters.read(ParameterCode.CFG_M05_01)).thenReturn(value);
        when(staff.assignedFloorManager(any(Long.class), any())).thenReturn(Optional.of(TestUsers.nextId()));
        when(staff.onDutyDoctors(any())).thenReturn(List.of());
        service = new AlertEscalationService(alerts, published::add, parameters, transaction, staff, null);
    }

    @Test
    void BR_M05_01_overdueMediumGoesUpOneLevelWithNewDeadline() {
        long id = insert("ESCALATED", 0, NOW.minusSeconds(60));

        assertThat(service.escalate(id, NOW)).isTrue();

        var row = row(id);
        assertThat(row.status()).isEqualTo("ESCALATED");
        assertThat(row.level()).isEqualTo(1);
        assertThat(row.deadline()).isEqualTo(NOW.plus(INTERVAL));
        assertThat(published).singleElement().isInstanceOfSatisfying(AlertEscalated.class, event -> {
            assertThat(event.sourceType()).isEqualTo("M05.ALERT");
            assertThat(event.sourceId()).isEqualTo(id);
            assertThat(event.escalationLevel()).isEqualTo(1);
            assertThat(event.notificationRequests()).hasSize(1);
        });
    }

    private long insert(String status, int level, Instant deadline) {
        long resident = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO alert (resident_id, floor_area_id, type, type_lock, level, first_at, latest_at, status,
                  escalation_level, acknowledgement_deadline, version)
                VALUES (?, ?, 'CHI_SO', 'MONITORING_ALERT:SPO2', 'MEDIUM', ?, ?, ?, ?, ?, 0)
                """,
                resident,
                TestUsers.nextId(),
                java.sql.Timestamp.from(NOW.minus(Duration.ofDays(1))),
                java.sql.Timestamp.from(NOW.minus(Duration.ofDays(1))),
                status,
                level,
                java.sql.Timestamp.from(deadline));
        return jdbc.queryForObject("SELECT id FROM alert WHERE resident_id = ?", Long.class, resident);
    }

    private Row row(long id) {
        var r = alerts.findById(id).orElseThrow();
        return new Row(
                r.status().name(),
                r.escalationLevel(),
                r.acknowledgementDeadline(),
                r.maxEscalationAt(),
                r.maxReminderCount());
    }

    private record Row(String status, int level, Instant deadline, Instant maximumAt, int reminders) {}
}
