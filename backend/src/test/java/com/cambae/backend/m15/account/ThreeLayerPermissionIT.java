package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.PermissionMatrixIT.FLOOR_1;
import static com.cambae.backend.m15.account.PermissionMatrixIT.FLOOR_2;
import static com.cambae.backend.m15.account.PermissionMatrixIT.resident;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionBasis;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T018 (US1 #1, #3, #4, #8, #10; BR-M15-01, FR-041 → FR-044; research A-10): ba lớp vai trò ∩ phạm vi ∩ pháp lý
 * qua {@code PermissionApi}, danh sách lệnh được phép, kiểm lại ở mỗi lần gọi, Bộ lập lịch.
 */
@Import(FakeM15Config.class)
class ThreeLayerPermissionIT extends MySqlIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(940_000);

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
    void US1_1_noCell_denied_andNotInAllowedCommands() {
        CurrentUser kitchen = staffOnFloor1(Role.KITCHEN_STAFF);
        PermissionKey createProfile = key(Function.RESIDENT_PROFILE, Operation.CREATE);

        assertThat(permissions.hasPermission(kitchen, createProfile, onFloor1).failedLayer())
                .contains(FailedLayer.ROLE);
        assertThat(permissions.allowedCommands(
                        kitchen,
                        onFloor1,
                        Map.of(
                                "create",
                                createProfile,
                                "reportIncident",
                                key(Function.INCIDENT_EMERGENCY, Operation.CREATE))))
                .containsExactly("reportIncident");
    }

    @Test
    void US1_3_facilityView_anyFloor_withoutAssignment() {
        CurrentUser manager = staff(Role.NURSING_HOME_MANAGER);
        PermissionKey view = key(Function.RESIDENT_PROFILE, Operation.VIEW);

        assertThat(permissions.hasPermission(manager, view, onFloor1).allowed()).isTrue();
        assertThat(permissions.hasPermission(manager, view, onFloor2).allowed()).isTrue();
    }

    @Test
    void US1_4_professionalOperation_deniedByLegalLayer_viewStillAllowed() {
        CurrentUser doctor = staff(Role.DOCTOR);

        assertThat(permissions
                        .hasPermission(doctor, key(Function.PRESCRIPTION, Operation.PROFESSIONAL), onFloor1)
                        .failedLayer())
                .contains(FailedLayer.LEGAL);
        assertThat(permissions
                        .hasPermission(doctor, key(Function.PRESCRIPTION, Operation.VIEW), onFloor1)
                        .allowed())
                .isTrue();
    }

    @Test
    void US1_8_permissionRevoked_nextCheckDenied() {
        CurrentUser caregiver = staffOnFloor1(Role.CAREGIVER);
        PermissionKey record = key(Function.TASK_CHECKLIST, Operation.CREATE);
        assertThat(permissions.hasPermission(caregiver, record, onFloor1).allowed())
                .isTrue();

        jdbc.update("""
                INSERT INTO permission_restriction (applies_to, account_id, function_code, operation, kind,
                                                    effective_from, actor_id, reason)
                VALUES ('ACCOUNT', ?, 'TASK_CHECKLIST', 'CREATE', 'REMOVE_OPERATION', ?, 1, 'Test')
                """, caregiver.accountId(), utc(time.now().minus(Duration.ofSeconds(1))));
        jdbc.update(
                "UPDATE account SET permission_version = permission_version + 1 WHERE id = ?", caregiver.accountId());

        assertThat(permissions.hasPermission(caregiver, record, onFloor1).failedLayer())
                .contains(FailedLayer.ROLE);
    }

    @Test
    void US1_10_assignedExecution_otherFloorDenied_facilityExecution_anyFloor() {
        CurrentUser caregiver = staffOnFloor1(Role.CAREGIVER);
        PermissionKey record = key(Function.TASK_CHECKLIST, Operation.CREATE);
        CurrentUser office = staff(Role.OFFICE_STAFF);
        PermissionKey absence = key(Function.ABSENCE_RETURN, Operation.CREATE);

        assertThat(permissions.hasPermission(caregiver, record, onFloor1).allowed())
                .isTrue();
        assertThat(permissions.hasPermission(caregiver, record, onFloor2).failedLayer())
                .contains(FailedLayer.SCOPE);
        assertThat(permissions.hasPermission(office, absence, onFloor2).allowed())
                .isTrue();
    }

    @Test
    void BR_M15_01_assignedGrant_withoutSpecificTarget_isScopeDenied_facilityGrantAllowed() {
        CurrentUser caregiver = staffOnFloor1(Role.CAREGIVER);
        CurrentUser manager = staff(Role.NURSING_HOME_MANAGER);
        PermissionKey view = key(Function.RESIDENT_PROFILE, Operation.VIEW);

        assertThat(permissions
                        .hasPermission(caregiver, view, Target.UNSPECIFIED)
                        .failedLayer())
                .contains(FailedLayer.SCOPE);
        assertThat(permissions.hasPermission(manager, view, Target.UNSPECIFIED).allowed())
                .isTrue();
    }

    @Test
    void FR_044_scheduler_allowedWithSchedulerBasis() {
        CurrentUser scheduler = com.cambae.backend.support.FakePermissions.user(0, Role.SCHEDULER);

        assertThat(permissions
                        .hasPermission(scheduler, key(Function.RESIDENT_PROFILE, Operation.UPDATE), onFloor2)
                        .basis())
                .contains(PermissionBasis.SCHEDULER);
    }

    @Test
    void FR_042_permissions_listsKeysOfRole() {
        CurrentUser kitchen = staff(Role.KITCHEN_STAFF);

        assertThat(permissions.permissions(kitchen))
                .contains(key(Function.INCIDENT_EMERGENCY, Operation.CREATE))
                .doesNotContain(key(Function.RESIDENT_PROFILE, Operation.CREATE));
    }

    private static PermissionKey key(Function function, Operation operation) {
        return new PermissionKey(function, operation);
    }

    private CurrentUser staffOnFloor1(Role role) {
        CurrentUser user = staff(role);
        assignments.assign(
                user.staffId().orElseThrow(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        FLOOR_1,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        return user;
    }

    private CurrentUser staff(Role role) {
        long staffId = IDS.incrementAndGet();
        LocalDateTime created = utc(time.now().minus(Duration.ofDays(1)));
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "three-layer-it-" + staffId, staffId, created, created);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, ?, ?, 'Test')",
                accountId,
                role.code(),
                created);
        return new ActiveSession(
                accountId,
                AccountType.STAFF,
                Optional.of(staffId),
                Optional.empty(),
                Set.of(role),
                "three-layer-it",
                AccessChannel.WEB);
    }

    private static LocalDateTime utc(java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
