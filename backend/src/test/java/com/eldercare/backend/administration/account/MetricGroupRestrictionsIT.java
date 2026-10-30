package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions.Kind;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions.Restriction;
import com.eldercare.backend.administration.account.api.dto.ReportChannel;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * "Đổi api" 2026-10-24 cho 016: {@code PermissionApi.metricGroupRestrictions} (thu hẹp chi tiết theo nhóm chỉ tiêu, kênh,
 * 016 FR-010a) và {@code FieldVisibilityApi.canViewByRole} (mức xem theo vai trò, không xét phạm vi, 016 Q-196).
 */
@Import(FakeM15Config.class)
class MetricGroupRestrictionsIT extends MySqlIntegrationTestBase {

    @Autowired
    PermissionApi permissions;

    @Autowired
    FieldVisibilityApi visibility;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    private Long roleRestrictionId;

    @AfterEach
    void closeRoleRestriction() {
        if (roleRestrictionId != null) {
            jdbc.update("UPDATE permission_assignment SET effective_to = ? WHERE id = ?", now(), roleRestrictionId);
        }
    }

    @Test
    void FR_010a_detailedRestrictions_byAccountAndByRole_keyLevelRowsExcluded() {
        TestUser office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);
        restrict("ACCOUNT", null, office.accountId(), "REMOVE_OPERATION", "BEDS", "DASHBOARD");
        restrict("ACCOUNT", null, office.accountId(), "LOWER_SCOPE", "RESIDENTS", null);
        roleRestrictionId = restrict("ROLE", "AC-09", null, "REMOVE_OPERATION", null, "REPORT");

        MetricGroupRestrictions r = permissions.metricGroupRestrictions(office.user());

        assertThat(r.byAccount())
                .containsExactlyInAnyOrder(
                        new Restriction("BEDS", ReportChannel.DASHBOARD, Kind.REMOVE_OPERATION),
                        new Restriction("RESIDENTS", null, Kind.LOWER_SCOPE));
        assertThat(r.byRole().get(Role.OFFICE_STAFF))
                .containsExactly(new Restriction(null, ReportChannel.REPORT, Kind.REMOVE_OPERATION));
        assertThat(r.byRole().get(Role.OFFICE_STAFF).getFirst().appliesTo("BEDS", ReportChannel.DASHBOARD))
                .isFalse();
    }

    @Test
    void FR_010a_noDetailedRestriction_empty() {
        TestUser manager = new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER);
        restrict("ACCOUNT", null, manager.accountId(), "REMOVE_OPERATION", null, null);

        MetricGroupRestrictions r = permissions.metricGroupRestrictions(manager.user());

        assertThat(r.byAccount()).isEmpty();
        assertThat(r.byRole()).isEmpty();
    }

    @Test
    void Q_196_canViewByRole_ignoresCurrentScope() {
        TestUsers users = new TestUsers(jdbc, time);

        assertThat(visibility.canViewByRole(users.staff(Role.NURSING_STAFF).user(), InfoGroup.HEALTH))
                .isEqualTo(Visibility.FULL);
        assertThat(visibility.canViewByRole(users.staff(Role.FLOOR_MANAGER).user(), InfoGroup.MEDICATION))
                .isEqualTo(Visibility.FULL);
        assertThat(visibility.canViewByRole(users.staff(Role.OFFICE_STAFF).user(), InfoGroup.HEALTH))
                .isEqualTo(Visibility.NONE);
        assertThat(visibility.canViewByRole(users.family(TestUsers.nextId()).user(), InfoGroup.HEALTH))
                .isEqualTo(Visibility.NONE);
    }

    private long restrict(String appliesTo, String role, Long accountId, String kind, String group, String channel) {
        jdbc.update(
                """
                INSERT INTO permission_assignment (kind, applies_to, role, account_id, function_code, operation,
                                                   restriction_kind, metric_group, channel, effective_from, actor_id,
                                                   reason)
                VALUES ('RESTRICTION', ?, ?, ?, 'DASHBOARD_REPORT', 'VIEW', ?, ?, ?, ?, 1, 'Test')
                """,
                appliesTo,
                role,
                accountId,
                kind,
                group,
                channel,
                LocalDateTime.ofInstant(time.now().minusSeconds(60), ZoneOffset.UTC));
        return jdbc.queryForObject("SELECT MAX(id) FROM permission_assignment WHERE kind = 'RESTRICTION'", Long.class);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
    }
}
