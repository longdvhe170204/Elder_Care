package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionBasis;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.m15.account.internal.permission.FootnoteRules;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T018, SC-001 (US1 #1, #2, #3, #10; BR-M15-01, FR-016 → FR-020): mỗi vai trò nhân viên được phân công tầng 1; với
 * mọi chức năng × loại thao tác, thử trên người cao tuổi A (tầng 1) và B (tầng 2), đối chiếu với ma trận đã nạp:
 *
 * <ul>
 *   <li>không có ô: từ chối lớp {@code ROLE} cả hai tầng;
 *   <li>ô có chú thích điều kiện hoặc nhiệm vụ ({@link FootnoteRules}): từ chối lớp {@code ROLE} (chưa có bản cài điều
 *       kiện; quyền theo nhiệm vụ đến từ lớp nhiệm vụ, US4);
 *   <li>ô toàn viện: được phép cả hai tầng; ô theo phân công: A được phép, B từ chối lớp {@code SCOPE}, trừ khóa
 *       {@code (INCIDENT_EMERGENCY, CREATE)}: miễn phạm vi, căn cứ ngoài phạm vi khi vai trò không xem được hồ sơ người
 *       đó (FR-044a).
 * </ul>
 *
 * <p>Không gồm: thao tác {@code PROFESSIONAL} (lớp pháp lý, US5 T061), Người thân AC-10 (phạm vi theo quan hệ, US6).
 */
@Import(FakeM15Config.class)
class PermissionMatrixIT extends MySqlIntegrationTestBase {

    static final long FLOOR_1 = 1;
    static final long FLOOR_2 = 2;
    private static final AtomicLong IDS = new AtomicLong(930_000);
    private static final List<Role> STAFF_ROLES = List.of(
            Role.NURSING_HOME_MANAGER,
            Role.FLOOR_MANAGER,
            Role.DOCTOR,
            Role.NURSING_STAFF,
            Role.CAREGIVER,
            Role.KITCHEN_STAFF,
            Role.OFFICE_STAFF,
            Role.ACCOUNTANT);
    private static final PermissionKey INCIDENT_CREATE =
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE);
    private static final PermissionKey PROFILE_VIEW = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    @Autowired
    PermissionApi permissions;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void SC_001_everyRoleFunctionOperation_onFloor1AndFloor2_matchesMatrix() {
        long residentA = IDS.incrementAndGet();
        long residentB = IDS.incrementAndGet();
        bedAllocations.allocate(residentA, new BedAllocationView(IDS.incrementAndGet(), 11, 101, FLOOR_1));
        bedAllocations.allocate(residentB, new BedAllocationView(IDS.incrementAndGet(), 21, 201, FLOOR_2));
        Target onFloor1 = resident(residentA);
        Target onFloor2 = resident(residentB);
        List<String> mismatches = new ArrayList<>();
        int checks = 0;

        for (Role role : STAFF_ROLES) {
            CurrentUser user = staffOnFloor1(role);
            Map<PermissionKey, Map<String, Object>> cells = matrix(role);
            for (Function function : Function.values()) {
                for (Operation operation : Operation.values()) {
                    if (operation == Operation.PROFESSIONAL) {
                        continue;
                    }
                    PermissionKey key = new PermissionKey(function, operation);
                    Map<String, Object> cell = cells.get(key);
                    for (Target target : List.of(onFloor1, onFloor2)) {
                        String expected = expected(key, cells, target == onFloor2);
                        String actual = describe(permissions.hasPermission(user, key, target));
                        checks++;
                        if (!expected.equals(actual)) {
                            mismatches.add(role.code() + " " + key + (target == onFloor1 ? " tầng 1" : " tầng 2")
                                    + ": mong đợi " + expected + ", thực tế " + actual);
                        }
                    }
                }
            }
        }

        assertThat(checks).isEqualTo(STAFF_ROLES.size() * Function.values().length * 6 * 2);
        assertThat(mismatches).isEmpty();
    }

    private static String expected(
            PermissionKey key, Map<PermissionKey, Map<String, Object>> cells, boolean otherFloor) {
        Map<String, Object> cell = cells.get(key);
        // Chưa module nào cài FootnoteCondition: chú thích điều kiện và nhiệm vụ đều chặn ở lớp vai trò
        if (cell == null
                || footnotes(cell).stream()
                        .anyMatch(c -> FootnoteRules.CONDITIONS.contains(c) || FootnoteRules.DUTIES.contains(c))) {
            return "DENIED ROLE";
        }
        if (key.equals(INCIDENT_CREATE)) {
            // FR-044a: miễn lớp phạm vi; ngoài phạm vi khi không xem được hồ sơ người đó (chủ dự án chọn, US1 #9)
            return expected(PROFILE_VIEW, cells, otherFloor).startsWith("ALLOWED")
                    ? "ALLOWED ROLE"
                    : "ALLOWED OUT_OF_SCOPE";
        }
        if ("FACILITY".equals(cell.get("scope")) || !otherFloor) {
            return "ALLOWED ROLE";
        }
        return "DENIED SCOPE";
    }

    private static String describe(PermissionResult r) {
        return r.allowed()
                ? "ALLOWED " + r.basis().map(PermissionBasis::kind).orElseThrow()
                : "DENIED " + r.failedLayer().map(FailedLayer::name).orElseThrow();
    }

    private Map<PermissionKey, Map<String, Object>> matrix(Role role) {
        Map<PermissionKey, Map<String, Object>> cells = new HashMap<>();
        jdbc.queryForList(
                        "SELECT function_code, operation, scope, footnote_codes FROM permission_matrix WHERE role = ?",
                        role.code())
                .forEach(row -> cells.put(
                        new PermissionKey(
                                Function.valueOf((String) row.get("function_code")),
                                Operation.valueOf((String) row.get("operation"))),
                        row));
        return cells;
    }

    private static Set<String> footnotes(Map<String, Object> cell) {
        String json = cell.get("footnote_codes").toString().replaceAll("[\\[\\]\" ]", "");
        return json.isEmpty() ? Set.of() : Set.of(json.split(","));
    }

    private CurrentUser staffOnFloor1(Role role) {
        long staffId = IDS.incrementAndGet();
        LocalDateTime created = LocalDateTime.ofInstant(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "matrix-it-" + staffId, staffId, created, created);
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
                        Instant.now().minus(Duration.ofDays(1)),
                        null));
        return new ActiveSession(
                accountId,
                AccountType.STAFF,
                Optional.of(staffId),
                Optional.empty(),
                Set.of(role),
                "matrix-it",
                AccessChannel.WEB);
    }

    static Target resident(long residentId) {
        return new Target("M01.RESIDENT", String.valueOf(residentId), residentId, null);
    }
}
