package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope.ASSIGNED;
import static com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope.FACILITY;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.internal.permission.domain.GrantSource;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleGrant;
import com.eldercare.backend.administration.account.internal.permission.service.RolePermissionCalculator;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T013 (research A-05; FR-025, FR-026a, BR-M15-18): quyền vai trò của tài khoản theo thứ tự ma trận trừ thu hẹp vai
 * trò → hợp các vai trò (toàn viện thắng) → trừ thu hẹp tài khoản → cộng quyền gán thêm; tính được tại thời điểm
 * {@code t}; bộ đệm theo {@code permission_version} của tài khoản và các vai trò.
 */
class RolePermissionCalculatorIT extends MySqlIntegrationTestBase {

    private static final AtomicLong STAFF_IDS = new AtomicLong(920_000);
    private static final PermissionKey RESIDENT_PROFILE_VIEW =
            new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);
    private static final PermissionKey BED_READINESS_CREATE =
            new PermissionKey(Function.BED_READINESS, Operation.CREATE);
    private static final PermissionKey DAY_CARE_ATTENDANCE_VIEW =
            new PermissionKey(Function.DAY_CARE_ATTENDANCE, Operation.VIEW);
    private static final PermissionKey CARE_PLAN_APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);
    private static final PermissionKey DASHBOARD_REPORT_VIEW =
            new PermissionKey(Function.DASHBOARD_REPORT, Operation.VIEW);

    @Autowired
    RolePermissionCalculator calculator;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void FR_016_singleRole_isExactlyItsMatrixRows() {
        long account = account("AC-03");

        Map<PermissionKey, Set<RoleGrant>> permissions = calculator.at(account, time.now());

        List<String> matrix = jdbc.queryForList(
                "SELECT CONCAT(function_code, '/', operation, '/', scope) FROM permission_matrix WHERE role = 'AC-03'",
                String.class);
        List<String> computed = permissions.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(g -> e.getKey().function() + "/" + e.getKey().operation() + "/" + g.scope()))
                .toList();
        assertThat(computed).containsExactlyInAnyOrderElementsOf(matrix);
    }

    @Test
    void FR_025_unionOfRoles_facilityWins_andConditionOfOneRoleNotForcedOnOther() {
        long account = account("AC-02", "AC-04");

        Map<PermissionKey, Set<RoleGrant>> permissions = calculator.at(account, time.now());

        assertThat(permissions.get(DAY_CARE_ATTENDANCE_VIEW)).containsExactly(role(FACILITY));
        assertThat(permissions.get(BED_READINESS_CREATE)).containsExactly(role(ASSIGNED));
    }

    @Test
    void BR_M15_18_roleRestriction_appliesBeforeUnion() {
        PermissionKey key = jdbc.queryForObject("""
                SELECT CONCAT(k.function_code, '/', k.operation) FROM permission_matrix k
                JOIN permission_matrix m ON m.function_code = k.function_code AND m.operation = k.operation
                    AND m.role = 'AC-01'
                WHERE k.role = 'AC-14' ORDER BY k.function_code, k.operation LIMIT 1
                """, (rs, i) -> parse(rs.getString(1)));
        long accountantOnly = account("AC-14");
        long accountantAndManager = account("AC-14", "AC-01");
        long restriction = restrict("ROLE", "AC-14", null, key, "REMOVE_OPERATION", null, ago(1), null);
        try {
            assertThat(calculator.at(accountantOnly, time.now())).doesNotContainKey(key);
            assertThat(calculator.at(accountantAndManager, time.now())).containsKey(key);
        } finally {
            jdbc.update(
                    "UPDATE permission_restriction SET effective_to = ? WHERE id = ?", utc(time.now()), restriction);
        }
    }

    @Test
    void FR_025_accountRestriction_lowerScope() {
        long account = account("AC-01");
        restrict("ACCOUNT", null, account, RESIDENT_PROFILE_VIEW, "LOWER_SCOPE", null, ago(1), null);

        assertThat(calculator.at(account, time.now()).get(RESIDENT_PROFILE_VIEW))
                .containsExactly(role(ASSIGNED));
    }

    @Test
    void BR_M15_18_accountRestriction_appliesAfterUnion() {
        long account = account("AC-02", "AC-04");
        restrict("ACCOUNT", null, account, BED_READINESS_CREATE, "REMOVE_OPERATION", null, ago(1), null);

        assertThat(calculator.at(account, time.now())).doesNotContainKey(BED_READINESS_CREATE);
    }

    @Test
    void FR_024_additionalGrant_addsCarePlanApprovalInAssignedScope() {
        long nurse = account("AC-04");
        assertThat(calculator.at(nurse, time.now())).doesNotContainKey(CARE_PLAN_APPROVE);
        jdbc.update("""
                INSERT INTO additional_grant (account_id, function_code, operation, effective_from, actor_id, reason)
                VALUES (?, 'CARE_PLAN', 'APPROVE', ?, 1, 'Test')
                """, nurse, utc(ago(1)));

        assertThat(calculator.at(nurse, time.now()).get(CARE_PLAN_APPROVE))
                .containsExactly(new RoleGrant(ASSIGNED, Set.of(), GrantSource.ADDITIONAL_GRANT));
    }

    @Test
    void FR_026a_permissionsAtPastInstant_useRowsEffectiveThen() {
        long account = account("AC-01");
        restrict("ACCOUNT", null, account, RESIDENT_PROFILE_VIEW, "REMOVE_OPERATION", null, ago(60), ago(30));

        assertThat(calculator.at(account, ago(45))).doesNotContainKey(RESIDENT_PROFILE_VIEW);
        assertThat(calculator.at(account, time.now())).containsKey(RESIDENT_PROFILE_VIEW);
    }

    @Test
    void dashboardRestrictionByMetricGroup_notAppliedAtKeyLevel() {
        long account = account("AC-01");
        restrict("ACCOUNT", null, account, DASHBOARD_REPORT_VIEW, "REMOVE_OPERATION", "OCCUPANCY", ago(1), null);

        assertThat(calculator.at(account, time.now())).containsKey(DASHBOARD_REPORT_VIEW);
    }

    @Test
    void FR_027_cache_recomputedOnlyWhenPermissionVersionChanges() {
        long account = account("AC-01");
        assertThat(calculator.current(account)).containsKey(RESIDENT_PROFILE_VIEW);
        restrict("ACCOUNT", null, account, RESIDENT_PROFILE_VIEW, "REMOVE_OPERATION", null, ago(1), null);

        assertThat(calculator.current(account)).containsKey(RESIDENT_PROFILE_VIEW);

        jdbc.update("UPDATE account SET permission_version = permission_version + 1 WHERE id = ?", account);
        assertThat(calculator.current(account)).doesNotContainKey(RESIDENT_PROFILE_VIEW);
    }

    private long account(String... roles) {
        long staffId = STAFF_IDS.incrementAndGet();
        LocalDateTime created = utc(ago(24 * 60));
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "role-permission-it-" + staffId, staffId, created, created);
        long id = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        for (String role : roles) {
            jdbc.update(
                    "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, ?, ?, 'Test')",
                    id,
                    role,
                    created);
        }
        return id;
    }

    private long restrict(
            String appliesTo,
            String role,
            Long accountId,
            PermissionKey key,
            String kind,
            String metricGroup,
            Instant from,
            Instant to) {
        jdbc.update(
                """
                INSERT INTO permission_restriction (applies_to, role, account_id, function_code, operation, kind,
                                                    metric_group, effective_from, effective_to, actor_id, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'Test')
                """,
                appliesTo,
                role,
                accountId,
                key.function().name(),
                key.operation().name(),
                kind,
                metricGroup,
                utc(from),
                to == null ? null : utc(to));
        return jdbc.queryForObject("SELECT MAX(id) FROM permission_restriction", Long.class);
    }

    private static RoleGrant role(PermissionScope scope) {
        return new RoleGrant(scope, Set.of(), GrantSource.ROLE);
    }

    private static PermissionKey parse(String key) {
        String[] parts = key.split("/");
        return new PermissionKey(Function.valueOf(parts[0]), Operation.valueOf(parts[1]));
    }

    private Instant ago(long minutes) {
        return time.now().minus(Duration.ofMinutes(minutes));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
