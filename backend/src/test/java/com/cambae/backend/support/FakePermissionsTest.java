package com.cambae.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.AccountType;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FailedLayer;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.ScopeFilter;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.api.Visibility;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    void approvers_areAccountsWithPermission() {
        FakePermissions fake = new FakePermissions()
                .grant(Role.NURSING_HOME_MANAGER, CHANGE_PARAMETER)
                .account(FakePermissions.user(1, Role.NURSING_HOME_MANAGER))
                .account(FakePermissions.user(2, Role.NURSING_STAFF));

        assertThat(fake.approvers(CHANGE_PARAMETER, Target.UNSPECIFIED)).containsExactly(1L);
        assertThat(fake.hasAnyApprover(CHANGE_PARAMETER, Target.UNSPECIFIED)).isTrue();
        assertThat(fake.hasRole(2, Role.NURSING_STAFF, Role.DOCTOR)).isTrue();
        assertThat(fake.hasRole(2, Role.DOCTOR)).isFalse();
    }

    @Test
    void scopeAndFieldVisibility_defaultToFullForManagerOnly() {
        FakePermissions fake = new FakePermissions();

        assertThat(fake.inScope(FakePermissions.user(1, Role.CAREGIVER), Target.UNSPECIFIED))
                .isTrue();
        assertThat(fake.canView(FakePermissions.user(1, Role.NURSING_HOME_MANAGER), null, Target.UNSPECIFIED))
                .isEqualTo(Visibility.FULL);
        assertThat(fake.canView(FakePermissions.user(2, Role.OFFICE_STAFF), null, Target.UNSPECIFIED))
                .isEqualTo(Visibility.NONE);
        assertThat(Role.RESIDENT.code()).isEqualTo("AC-16");
        assertThat(Set.of(Role.values())).hasSize(11);
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

    @Test
    void T008_userType_followsRoles() {
        assertThat(FakePermissions.user(1, Role.FAMILY_MEMBER).familyMemberId()).contains(1L);
        assertThat(FakePermissions.user(2, Role.RESIDENT).type()).isEqualTo(AccountType.RESIDENT);
        assertThat(FakePermissions.user(3, Role.DOCTOR).staffId()).contains(3L);
    }
}
