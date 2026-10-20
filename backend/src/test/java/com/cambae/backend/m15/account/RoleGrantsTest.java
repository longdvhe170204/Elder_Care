package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.internal.permission.domain.GrantSource.ROLE;
import static com.cambae.backend.m15.account.internal.permission.domain.PermissionScope.ASSIGNED;
import static com.cambae.backend.m15.account.internal.permission.domain.PermissionScope.FACILITY;
import static com.cambae.backend.m15.account.internal.permission.domain.PermissionScope.RELATIONSHIP;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.internal.permission.domain.PermissionScope;
import com.cambae.backend.m15.account.internal.permission.domain.RoleGrant;
import com.cambae.backend.m15.account.internal.permission.domain.RoleGrants;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 002 T013 (research A-05; FR-025): hợp quyền các vai trò trên cùng một khóa giữ các phương án (phạm vi, chú thích); bỏ
 * phương án bị phủ (phạm vi rộng bằng hoặc hơn và ít điều kiện bằng hoặc hơn), nên toàn viện thắng mà không gán điều
 * kiện của vai trò này cho vai trò kia.
 */
class RoleGrantsTest {

    private static RoleGrant grant(PermissionScope scope, String... footnotes) {
        return new RoleGrant(scope, Set.of(footnotes), ROLE);
    }

    @Test
    void FR_025_facilityWins_overAssignedWithSameOrMoreConditions() {
        assertThat(RoleGrants.add(Set.of(grant(ASSIGNED, "27")), grant(FACILITY)))
                .containsExactly(grant(FACILITY));
        assertThat(RoleGrants.add(Set.of(grant(FACILITY)), grant(ASSIGNED))).containsExactly(grant(FACILITY));
    }

    @Test
    void FR_025_sameScope_unconditionalWins_conditionOfOtherRoleNotAdded() {
        assertThat(RoleGrants.add(Set.of(grant(ASSIGNED)), grant(ASSIGNED, "40")))
                .containsExactly(grant(ASSIGNED));
        assertThat(RoleGrants.add(Set.of(grant(ASSIGNED, "40")), grant(ASSIGNED)))
                .containsExactly(grant(ASSIGNED));
    }

    @Test
    void FR_025_widerWithConditionAndNarrowerWithout_bothKept() {
        assertThat(RoleGrants.add(Set.of(grant(FACILITY, "11")), grant(ASSIGNED)))
                .containsExactlyInAnyOrder(grant(FACILITY, "11"), grant(ASSIGNED));
    }

    @Test
    void FR_025_lowerScope_facilityBecomesAssigned_othersUnchanged() {
        assertThat(RoleGrants.lower(Set.of(grant(FACILITY, "5")))).containsExactly(grant(ASSIGNED, "5"));
        assertThat(RoleGrants.lower(Set.of(grant(RELATIONSHIP)))).containsExactly(grant(RELATIONSHIP));
        assertThat(RoleGrants.lower(Set.of(grant(FACILITY, "11"), grant(ASSIGNED))))
                .containsExactly(grant(ASSIGNED));
    }
}
