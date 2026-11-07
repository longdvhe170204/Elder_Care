package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssignmentFixtureIsolationIT extends AssignmentHttpTestBase {
    @Test
    void FR_055_fixtureCleanupLeavesNoOpenFutureRolesForFollowingTests() {
        reset();
        for (long account : fixtureAccounts) {
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM permission_assignment WHERE kind='ROLE_ASSIGNMENT' AND account_id=? AND effective_to IS NULL",
                            Integer.class,
                            account))
                    .isZero();
        }
    }
}
