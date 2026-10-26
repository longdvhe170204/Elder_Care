package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.fake.m15.FakeStaffAbsences;
import org.junit.jupiter.api.Test;

class AbsenceSourceContractsTest {
    @Test
    void FR_053_sharedAbsenceFakeDefaultsToDenyAndKeepsAccountAndAbsenceSeparate() {
        var fake = new FakeStaffAbsences();
        var user = FakePermissions.user(123, Role.NURSING_STAFF);
        assertThat(fake.canViewReason(user, 10)).isFalse();
        fake.allow(123, 10);
        assertThat(fake.canViewReason(user, 10)).isTrue();
        assertThat(fake.canViewReason(user, 11)).isFalse();
        assertThat(fake.canViewReason(FakePermissions.user(124, Role.NURSING_STAFF), 10))
                .isFalse();
        fake.clear();
        assertThat(fake.canViewReason(user, 10)).isFalse();
    }
}
