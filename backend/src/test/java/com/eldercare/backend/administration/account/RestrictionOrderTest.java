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
import com.eldercare.backend.administration.account.internal.permission.domain.RolePermissionRules;
import com.eldercare.backend.administration.account.internal.permission.domain.RolePermissionRules.RestrictionRule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 002 T073 (research A-05; FR-025, BR-M15-18, CHK017): thứ tự tính quyền vai trò. Thu hẹp theo vai trò áp lên quyền
 * của vai trò đó trước khi hợp, nên vai trò khác cùng đem lại quyền vẫn giữ; thu hẹp theo tài khoản áp sau khi hợp, nên
 * quyền bị bỏ dù vai trò nào đem lại; quyền gán thêm cộng sau cùng.
 */
class RestrictionOrderTest {

    private static final PermissionKey COST_CREATE = new PermissionKey(Function.COST_ADJUSTMENT, Operation.CREATE);
    private static final PermissionKey PROFILE_VIEW = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);
    private static final PermissionKey CARE_PLAN_APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);

    private static RoleGrant role(PermissionScope scope) {
        return new RoleGrant(scope, Set.of(), GrantSource.ROLE);
    }

    private static final Map<String, Map<PermissionKey, Set<RoleGrant>>> OFFICE_AND_NURSE = Map.of(
            "AC-09", Map.of(COST_CREATE, Set.of(role(FACILITY)), PROFILE_VIEW, Set.of(role(FACILITY))),
            "AC-04", Map.of(COST_CREATE, Set.of(role(ASSIGNED)), PROFILE_VIEW, Set.of(role(ASSIGNED))));

    @Test
    void BR_M15_18_roleRestriction_beforeUnion_otherRoleKeepsPermission() {
        Map<PermissionKey, Set<RoleGrant>> result = RolePermissionRules.combine(
                OFFICE_AND_NURSE, List.of(new RestrictionRule("AC-09", COST_CREATE, false)), List.of(), Set.of());

        assertThat(result.get(COST_CREATE)).containsExactly(role(ASSIGNED));
        assertThat(result.get(PROFILE_VIEW)).containsExactly(role(FACILITY));
    }

    @Test
    void BR_M15_18_roleLowerScope_beforeUnion_facilityOfOtherRoleWins() {
        Map<PermissionKey, Set<RoleGrant>> result = RolePermissionRules.combine(
                Map.of(
                        "AC-09", Map.of(PROFILE_VIEW, Set.of(role(FACILITY))),
                        "AC-03", Map.of(PROFILE_VIEW, Set.of(role(FACILITY)))),
                List.of(new RestrictionRule("AC-09", PROFILE_VIEW, true)),
                List.of(),
                Set.of());

        assertThat(result.get(PROFILE_VIEW)).containsExactly(role(FACILITY));
    }

    @Test
    void BR_M15_18_accountRestriction_afterUnion_removedWhicheverRoleGivesIt() {
        Map<PermissionKey, Set<RoleGrant>> result = RolePermissionRules.combine(
                OFFICE_AND_NURSE, List.of(), List.of(new RestrictionRule(null, COST_CREATE, false)), Set.of());

        assertThat(result).doesNotContainKey(COST_CREATE).containsKey(PROFILE_VIEW);
    }

    @Test
    void BR_M15_18_accountLowerScope_afterUnion_facilityBecomesAssigned() {
        Map<PermissionKey, Set<RoleGrant>> result = RolePermissionRules.combine(
                OFFICE_AND_NURSE, List.of(), List.of(new RestrictionRule(null, PROFILE_VIEW, true)), Set.of());

        assertThat(result.get(PROFILE_VIEW)).containsExactly(role(ASSIGNED));
    }

    @Test
    void FR_024_additionalGrant_addedLast_assignedScope() {
        Map<PermissionKey, Set<RoleGrant>> result = RolePermissionRules.combine(
                OFFICE_AND_NURSE,
                List.of(),
                List.of(new RestrictionRule(null, CARE_PLAN_APPROVE, false)),
                Set.of(CARE_PLAN_APPROVE));

        assertThat(result.get(CARE_PLAN_APPROVE))
                .containsExactly(new RoleGrant(ASSIGNED, Set.of(), GrantSource.ADDITIONAL_GRANT));
    }
}
