package com.eldercare.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Role;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 000 T025: bản giả quyền của 002 theo bảng vai trò → quyền tối thiểu. */
class FakePermissionsTest {

    private static final PermissionKey CHANGE_PARAMETER =
            new PermissionKey(Function.ACCOUNT_PERMISSION_PARAMETER, Operation.UPDATE);

    @Test
    void grantedRole_hasPermission_otherRoleDoesNot() {
        FakePermissions fake = new FakePermissions().grant(Role.NURSING_HOME_MANAGER, CHANGE_PARAMETER);

        assertThat(fake.hasPermission(
                                FakePermissions.user(1, Role.NURSING_HOME_MANAGER),
                                CHANGE_PARAMETER,
                                Target.UNSPECIFIED)
                        .allowed())
                .isTrue();
        assertThat(fake.hasPermission(FakePermissions.user(2, Role.NURSING_STAFF), CHANGE_PARAMETER, Target.UNSPECIFIED)
                        .allowed())
                .isFalse();
    }

    @Test
    void T008_reviewDecider_floorManagerThenShiftLeadThenManager() {
        FakePermissions fake = new FakePermissions()
                .account(FakePermissions.user(1, Role.NURSING_HOME_MANAGER))
                .floorManager(10, 5)
                .shiftLead(20, 6);

        assertThat(fake.reviewDecider(Optional.of(10L)).accountIds()).containsExactly(5L);
        assertThat(fake.reviewDecider(Optional.of(20L)).accountIds()).containsExactly(6L);
        assertThat(fake.reviewDecider(Optional.empty()).accountIds()).containsExactly(1L);
    }

    @Test
    void T008_allowedCommandsAndPermissions_followGrants() {
        FakePermissions fake = new FakePermissions().grant(Role.NURSING_HOME_MANAGER, CHANGE_PARAMETER);
        CurrentUser manager = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
        PermissionKey view = new PermissionKey(Function.ACCOUNT_PERMISSION_PARAMETER, Operation.VIEW);

        assertThat(fake.permissions(manager)).containsExactly(CHANGE_PARAMETER);
        assertThat(fake.allowedCommands(manager, Target.UNSPECIFIED, Map.of("change", CHANGE_PARAMETER, "view", view)))
                .containsExactly("change");
        assertThat(fake.hasPermission(FakePermissions.user(2, Role.CAREGIVER), view, Target.UNSPECIFIED)
                        .failedLayer())
                .contains(FailedLayer.ROLE);
        assertThat(fake.filter(manager, CHANGE_PARAMETER)).isEqualTo(ScopeFilter.FACILITY);
    }
}
