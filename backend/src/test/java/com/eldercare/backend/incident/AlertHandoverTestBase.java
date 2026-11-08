package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.persistence.HandoverItemStore;
import com.eldercare.backend.incident.internal.alert.service.AlertAssigneeTransfer;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dữ liệu chung cho test bàn giao của 007 (FR-037, Q-94): bảng cảnh báo, sự cố thật trên MySQL; lịch ca, tài khoản,
 * Người phụ trách ca là bản giả Mockito. Mã tài khoản trùng mã nhân viên cho dễ đọc. Mỗi test một tầng riêng nên không
 * thấy dữ liệu sót của test khác.
 */
abstract class AlertHandoverTestBase extends MySqlIntegrationTestBase {
    static final Instant NOW = Instant.parse("2030-05-06T14:05:00Z");
    static final Instant DEADLINE = NOW.plusSeconds(600);

    final long PREVIOUS_SHIFT = TestUsers.nextId();
    final long NEXT_SHIFT = TestUsers.nextId();
    final long DAY_NURSE = TestUsers.nextId();
    final long NIGHT_NURSE = TestUsers.nextId();
    final long OTHER_NURSE = TestUsers.nextId();
    final long DOCTOR = TestUsers.nextId();
    final long MANAGER = TestUsers.nextId();
    final long SHIFT_LEAD = TestUsers.nextId();
    final long FLOOR_MANAGER = TestUsers.nextId();

    @Autowired
    HandoverItemStore items;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    final BusinessTime time = mock(BusinessTime.class);
    final AlertStaffPort staff = mock(AlertStaffPort.class);
    final AccountApi accounts = mock(AccountApi.class);
    final ScopeApi scopes = mock(ScopeApi.class);
    final List<Object> published = new ArrayList<>();
    AlertAssigneeTransfer transfer;
    long floor;
    long resident;

    @BeforeEach
    void handoverFixture() {
        floor = TestUsers.nextId();
        resident = TestUsers.nextId();
        when(time.now()).thenReturn(NOW);
        for (long id : List.of(DAY_NURSE, NIGHT_NURSE, OTHER_NURSE, DOCTOR, MANAGER, SHIFT_LEAD, FLOOR_MANAGER)) {
            var view = new AccountView(id, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(), id);
            when(accounts.byStaff(id)).thenReturn(Optional.of(view));
            when(accounts.byId(id)).thenReturn(Optional.of(view));
        }
        when(accounts.hasRole(eq(DOCTOR), any(Role[].class))).thenReturn(true);
        when(accounts.hasRole(eq(MANAGER), any(Role[].class))).thenReturn(true);
        for (long id : List.of(DAY_NURSE, DOCTOR, MANAGER)) {
            when(staff.listedInShift(id, PREVIOUS_SHIFT)).thenReturn(true);
        }
        when(staff.assignedNurse(eq(resident), any())).thenReturn(Optional.of(NIGHT_NURSE));
        when(scopes.shiftLead(eq(floor), any())).thenReturn(Optional.of(SHIFT_LEAD));
        when(staff.assignedFloorManager(anyLong(), any())).thenReturn(Optional.of(FLOOR_MANAGER));
        var responsibility = new AlertResponsibility(staff, accounts, scopes, null, null);
        transfer = new AlertAssigneeTransfer(items, responsibility, accounts, published::add, staff, null);
    }

    void noNightNurse() {
        when(staff.assignedNurse(eq(resident), any())).thenReturn(Optional.empty());
    }

    long insertAlert(String status, long assignee, boolean provisional) {
        jdbc.update(
                """
                INSERT INTO alert (resident_id, floor_area_id, type, type_lock, level, first_at, latest_at, status,
                  escalation_level, acknowledgement_deadline, assignee_id, handover_acknowledged, version)
                VALUES (?, ?, 'CHI_SO', ?, 'MEDIUM', ?, ?, ?, 1, ?, ?, ?, 0)
                """,
                resident,
                floor,
                "VUOT_NGUONG:" + TestUsers.nextId(),
                Timestamp.from(NOW.minusSeconds(3600)),
                Timestamp.from(NOW.minusSeconds(3600)),
                status,
                Timestamp.from(DEADLINE),
                assignee,
                provisional);
        return jdbc.queryForObject("SELECT MAX(id) FROM alert WHERE floor_area_id = ?", Long.class, floor);
    }

    long insertIncident(String status, long assignee, boolean provisional) {
        long type = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO incident (resident_id, incident_type_id, current_type_id, type_code, discovered_at,
                  floor_area_id, description, initial_level, recorded_by, created_at, confirmed_at, status, level,
                  assignee_id, handover_acknowledged, version)
                VALUES (?, ?, ?, 'OTHER', ?, ?, 'Ngã nhẹ', 'MEDIUM', ?, ?, ?, ?, 'MEDIUM', ?, ?, 0)
                """,
                resident,
                type,
                type,
                Timestamp.from(NOW.minusSeconds(3600)),
                floor,
                DAY_NURSE,
                Timestamp.from(NOW.minusSeconds(3600)),
                Timestamp.from(NOW.minusSeconds(3600)),
                status,
                assignee,
                provisional);
        return jdbc.queryForObject("SELECT MAX(id) FROM incident WHERE floor_area_id = ?", Long.class, floor);
    }

    AlertRow alertRow(long id) {
        return jdbc.queryForObject(
                "SELECT status, escalation_level, acknowledgement_deadline, assignee_id, handover_acknowledged"
                        + " FROM alert WHERE id = ?",
                (rs, n) -> new AlertRow(
                        rs.getString(1),
                        rs.getInt(2),
                        rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant(),
                        rs.getLong(4),
                        rs.getBoolean(5)),
                id);
    }

    long incidentAssignee(long id) {
        return jdbc.queryForObject("SELECT assignee_id FROM incident WHERE id = ?", Long.class, id);
    }

    boolean incidentProvisional(long id) {
        return jdbc.queryForObject("SELECT handover_acknowledged FROM incident WHERE id = ?", Boolean.class, id);
    }

    record AlertRow(String status, int escalationLevel, Instant deadline, long assignee, boolean provisional) {}
}
