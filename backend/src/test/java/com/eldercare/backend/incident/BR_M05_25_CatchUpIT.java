package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.incident.api.events.AlertEscalated;
import com.eldercare.backend.incident.api.events.MaximumEscalationReminder;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertEscalationService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
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
 * Chạy bù sau khi Bộ lập lịch ngừng (BR-M05-25, FR-084 a, d): mỗi cảnh báo quá hạn lên đúng một cấp, hạn mới tính từ lúc
 * chạy lại; lần nhắc "đã leo thang tối đa" bị lỡ chỉ gửi một lần, lần kế tiếp ở mốc tương lai.
 */
class BR_M05_25_CatchUpIT extends MySqlIntegrationTestBase {
    private static final Instant RESTART = Instant.parse("2031-01-02T03:00:00Z");
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
        service = new AlertEscalationService(alerts, published::add, parameters, transaction, staff, null);
    }

    @Test
    void BR_M05_25_eachOverdueAlertMovesOneLevelWithDeadlineFromRestart() {
        long first = insert(0, RESTART.minus(Duration.ofHours(3)), null);
        long second = insert(1, RESTART.minus(Duration.ofHours(2)), null);

        service.escalate(first, RESTART);
        service.escalate(second, RESTART);

        assertThat(level(first)).isEqualTo(1);
        assertThat(level(second)).isEqualTo(2);
        assertThat(deadline(first)).isEqualTo(RESTART.plus(INTERVAL));
        assertThat(published).hasSize(2).allMatch(AlertEscalated.class::isInstance);
    }

    @Test
    void FR_084_d_missedMaximumRemindersAreSentOnce() {
        Instant maximumAt = RESTART.minus(Duration.ofHours(2));
        long id = insert(2, maximumAt.plus(INTERVAL), maximumAt);

        service.escalate(id, RESTART);
        service.escalate(id, RESTART.plusSeconds(60));
        service.escalate(id, RESTART.plusSeconds(120));

        assertThat(published).singleElement().isInstanceOf(MaximumEscalationReminder.class);
        assertThat(deadline(id)).isAfter(RESTART.plusSeconds(120));
        assertThat(deadline(id)).isEqualTo(maximumAt.plus(INTERVAL.multipliedBy(9)));
    }

    private long insert(int level, Instant deadline, Instant maximumAt) {
        long resident = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO alert (resident_id, floor_area_id, type, type_lock, level, first_at, latest_at, status,
                  escalation_level, acknowledgement_deadline, max_escalation_at, version)
                VALUES (?, ?, 'CHI_SO', 'VUOT_NGUONG:1', 'MEDIUM', ?, ?, 'ESCALATED', ?, ?, ?, 0)
                """,
                resident,
                TestUsers.nextId(),
                Timestamp.from(RESTART.minus(Duration.ofDays(1))),
                Timestamp.from(RESTART.minus(Duration.ofDays(1))),
                level,
                Timestamp.from(deadline),
                maximumAt == null ? null : Timestamp.from(maximumAt));
        return jdbc.queryForObject("SELECT id FROM alert WHERE resident_id = ?", Long.class, resident);
    }

    private int level(long id) {
        return alerts.findById(id).orElseThrow().escalationLevel();
    }

    private Instant deadline(long id) {
        return alerts.findById(id).orElseThrow().acknowledgementDeadline();
    }
}
