package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.PermissionMatrixIT.FLOOR_1;
import static com.cambae.backend.m15.account.PermissionMatrixIT.resident;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.events.PermissionDataError;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.m15.account.internal.auth.service.SessionAuthentication;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 002 T024 (FR-041b; research A-10; CHK059): dữ liệu nguồn lỗi thì từ chối lớp {@code SOURCE_DATA}; bảng
 * {@code permission_data_error} gộp theo (nguồn, bản ghi, loại lỗi) đang mở, lần đầu phát {@code PermissionDataError}
 * báo Quản lý viện, các lần sau chỉ tăng bộ đếm; mỗi lần từ chối ghi nhật ký; lần kiểm tra kế tiếp đọc được dữ liệu
 * hợp lệ thì đánh dấu đã khắc phục. Bản ghi lỗi giữ lại dù lệnh bị từ chối rollback.
 */
@Import(FakeM15Config.class)
@RecordApplicationEvents
class PermissionDataErrorIT extends MySqlIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(970_000);
    private static final PermissionKey RECORD_TASK = new PermissionKey(Function.TASK_CHECKLIST, Operation.CREATE);

    @Autowired
    PermissionApi permissions;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ApplicationEvents events;

    ActiveSession caregiver;

    @BeforeEach
    void caregiverOnFloor1() {
        caregiver = staff(Role.CAREGIVER);
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication(caregiver));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void FR_041b_firstErrorReported_repeatsOnlyCounted_eachDenialAudited() {
        long broken = IDS.incrementAndGet();
        bedAllocations.invalid(broken, "TWO_OPEN_ALLOCATIONS");

        assertThat(permissions
                        .hasPermission(caregiver, RECORD_TASK, resident(broken))
                        .failedLayer())
                .contains(FailedLayer.SOURCE_DATA);
        permissions.hasPermission(caregiver, RECORD_TASK, resident(broken));

        Map<String, Object> error = openError(broken);
        assertThat(error)
                .containsEntry("source", "M03.BED_ALLOCATION")
                .containsEntry("error_kind", "TWO_OPEN_ALLOCATIONS")
                .containsEntry("occurrences", 2);
        assertThat(events.stream(PermissionDataError.class).filter(e -> e.recordId() == broken))
                .containsExactly(new PermissionDataError("M03.BED_ALLOCATION", broken, "TWO_OPEN_ALLOCATIONS"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'PERMISSION_DENIED_SOURCE_DATA'"
                                + " AND entity_type = 'M15.PERMISSION_DATA_ERROR' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(error.get("id"))))
                .isEqualTo(2);
    }

    @Test
    void CHK059_nextCheckWithValidData_marksFixed_newErrorOpensNewRow() {
        long resident = IDS.incrementAndGet();
        bedAllocations.invalid(resident, "TWO_OPEN_ALLOCATIONS");
        permissions.hasPermission(caregiver, RECORD_TASK, resident(resident));

        bedAllocations.allocate(resident, new BedAllocationView(IDS.incrementAndGet(), 11, 101, FLOOR_1));
        assertThat(permissions
                        .hasPermission(caregiver, RECORD_TASK, resident(resident))
                        .allowed())
                .isTrue();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM permission_data_error WHERE record_id = ? AND fixed_at IS NOT NULL",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        bedAllocations.invalid(resident, "TWO_OPEN_ALLOCATIONS");
        permissions.hasPermission(caregiver, RECORD_TASK, resident(resident));
        assertThat(openError(resident)).containsEntry("occurrences", 1);
        assertThat(events.stream(PermissionDataError.class).filter(e -> e.recordId() == resident))
                .hasSize(2);
    }

    @Test
    void FR_041b_errorKept_whenCommandTransactionRollsBack() {
        long broken = IDS.incrementAndGet();
        bedAllocations.invalid(broken, "MISSING_ALLOCATION");

        tx.executeWithoutResult(status -> {
            permissions.hasPermission(caregiver, RECORD_TASK, resident(broken));
            status.setRollbackOnly();
        });

        assertThat(openError(broken)).containsEntry("occurrences", 1);
    }

    private Map<String, Object> openError(long recordId) {
        return jdbc.queryForMap(
                "SELECT id, source, error_kind, occurrences FROM permission_data_error"
                        + " WHERE record_id = ? AND fixed_at IS NULL",
                recordId);
    }

    private ActiveSession staff(Role role) {
        long staffId = IDS.incrementAndGet();
        LocalDateTime created = LocalDateTime.ofInstant(time.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "data-error-it-" + staffId, staffId, created, created);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, ?, ?, 'Test')",
                accountId,
                role.code(),
                created);
        assignments.assign(
                staffId,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        FLOOR_1,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        return new ActiveSession(
                accountId,
                AccountType.STAFF,
                Optional.of(staffId),
                Optional.empty(),
                Set.of(role),
                "data-error-it",
                AccessChannel.WEB);
    }
}
