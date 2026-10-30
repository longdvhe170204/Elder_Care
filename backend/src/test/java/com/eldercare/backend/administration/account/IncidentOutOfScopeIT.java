package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.PermissionMatrixIT.FLOOR_1;
import static com.eldercare.backend.administration.account.PermissionMatrixIT.FLOOR_2;
import static com.eldercare.backend.administration.account.PermissionMatrixIT.resident;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.service.SessionAuthentication;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

/**
 * 002 T020 (FR-044a, FR-041b, US1 #9; research A-07, A-10): khóa {@code (INCIDENT_EMERGENCY, CREATE)} trên một người cao
 * tuổi bỏ lớp phạm vi, kết quả mang căn cứ ngoài phạm vi để lệnh của 007 ghi {@code ngoaiPhamVi} vào nhật ký; khóa khác
 * vẫn bị chặn; dữ liệu nguồn lỗi thì từ chối lớp {@code SOURCE_DATA}, riêng ghi sự cố vẫn được phép.
 */
@Import(FakeM15Config.class)
class IncidentOutOfScopeIT extends MySqlIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(960_000);
    private static final PermissionKey REPORT_INCIDENT =
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE);
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

    Target onFloor1;
    Target onFloor2;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void residents() {
        long a = IDS.incrementAndGet();
        long b = IDS.incrementAndGet();
        bedAllocations.allocate(a, new BedAllocationView(IDS.incrementAndGet(), 11, 101, FLOOR_1));
        bedAllocations.allocate(b, new BedAllocationView(IDS.incrementAndGet(), 21, 201, FLOOR_2));
        onFloor1 = resident(a);
        onFloor2 = resident(b);
    }

    @Test
    void US1_9_kitchenWithoutAssignment_reportsIncident_outOfScope_butCannotOpenProfile() {
        CurrentUser kitchen = staff(Role.KITCHEN_STAFF, null);

        assertThat(permissions.hasPermission(kitchen, REPORT_INCIDENT, onFloor1).basis())
                .contains(PermissionBasis.OUT_OF_SCOPE);
        assertThat(permissions
                        .hasPermission(kitchen, new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW), onFloor1)
                        .allowed())
                .isFalse();
    }

    @Test
    void FR_044a_inScope_basisIsRole_outOfScope_basisIsOutOfScope_otherKeysStillScoped() {
        CurrentUser caregiver = staff(Role.CAREGIVER, FLOOR_1);

        assertThat(permissions
                        .hasPermission(caregiver, REPORT_INCIDENT, onFloor1)
                        .basis())
                .contains(PermissionBasis.ROLE);
        assertThat(permissions
                        .hasPermission(caregiver, REPORT_INCIDENT, onFloor2)
                        .basis())
                .contains(PermissionBasis.OUT_OF_SCOPE);
        assertThat(permissions.hasPermission(caregiver, RECORD_TASK, onFloor2).failedLayer())
                .contains(FailedLayer.SCOPE);
    }

    @Test
    void FR_041b_sourceDataError_deniedAsSourceData_incidentStillAllowed() {
        CurrentUser caregiver = staff(Role.CAREGIVER, FLOOR_1);
        // Từ chối vì dữ liệu nguồn ghi nhật ký với người dùng hiện tại (T024)
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication((ActiveSession) caregiver));
        long broken = IDS.incrementAndGet();
        bedAllocations.invalid(broken, "TWO_OPEN_ALLOCATIONS");

        assertThat(permissions
                        .hasPermission(caregiver, RECORD_TASK, resident(broken))
                        .failedLayer())
                .contains(FailedLayer.SOURCE_DATA);
        assertThat(permissions
                        .hasPermission(caregiver, REPORT_INCIDENT, resident(broken))
                        .basis())
                .contains(PermissionBasis.OUT_OF_SCOPE);
    }

    private CurrentUser staff(Role role, Long floor) {
        long staffId = IDS.incrementAndGet();
        LocalDateTime created = LocalDateTime.ofInstant(time.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "incident-it-" + staffId, staffId, created, created);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        jdbc.update(
                "INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, reason)"
                        + " VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', ?, ?, ?, 'Test')",
                accountId,
                role.code(),
                created);
        if (floor != null) {
            assignments.assign(
                    staffId,
                    new AssignmentView(
                            AssignmentKind.SHIFT_MEMBER,
                            floor,
                            null,
                            null,
                            null,
                            time.now().minus(Duration.ofDays(1)),
                            null));
        }
        return new ActiveSession(
                accountId,
                AccountType.STAFF,
                Optional.of(staffId),
                Optional.empty(),
                Set.of(role),
                "incident-it",
                AccessChannel.WEB);
    }
}
