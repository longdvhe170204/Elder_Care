package com.eldercare.backend.family.internal.relationship.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.family.internal.relationship.domain.Dbr02Check.Roles;
import com.eldercare.backend.family.internal.relationship.domain.Dbr02Check.Violation;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 012 T005 (research F-05; FR-003, DBR-02): đúng một liên hệ chính, ít nhất một người đại diện. */
class Dbr02CheckTest {

    private static final Roles REPRESENTATIVE = new Roles(true, false);
    private static final Roles PRIMARY_CONTACT = new Roles(false, true);
    private static final Roles BOTH = new Roles(true, true);
    private static final Roles NONE = new Roles(false, false);

    @ParameterizedTest
    @EnumSource(
            value = ResidentStatus.class,
            names = {"IN_RESIDENCE", "TEMPORARILY_ABSENT", "HOSPITALIZED"})
    void DBR_02_residing_needsOnePrimaryContactAndARepresentative(ResidentStatus status) {
        assertThat(Dbr02Check.violations(status, List.of(BOTH))).isEmpty();
        assertThat(Dbr02Check.violations(status, List.of(REPRESENTATIVE, PRIMARY_CONTACT, NONE)))
                .isEmpty();
        assertThat(Dbr02Check.violations(status, List.of(REPRESENTATIVE)))
                .containsExactly(Violation.NO_PRIMARY_CONTACT);
        assertThat(Dbr02Check.violations(status, List.of(PRIMARY_CONTACT)))
                .containsExactly(Violation.NO_REPRESENTATIVE);
        assertThat(Dbr02Check.violations(status, List.of()))
                .containsExactly(Violation.NO_PRIMARY_CONTACT, Violation.NO_REPRESENTATIVE);
    }

    @Test
    void DBR_02_twoPrimaryContacts_isAViolation() {
        assertThat(Dbr02Check.violations(ResidentStatus.IN_RESIDENCE, List.of(BOTH, PRIMARY_CONTACT)))
                .containsExactly(Violation.MORE_THAN_ONE_PRIMARY_CONTACT);
    }

    @Test
    void FR_003_admissionInProgress_maySaveIncompleteRoles() {
        assertThat(Dbr02Check.violations(ResidentStatus.ADMISSION_IN_PROGRESS, List.of()))
                .isEmpty();
        assertThat(Dbr02Check.satisfied(List.of()))
                .as("điều kiện DBR_02 của Hoàn tất tiếp nhận xét đủ vai trò bất kể trạng thái")
                .isFalse();
        assertThat(Dbr02Check.satisfied(List.of(BOTH))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = ResidentStatus.class,
            names = {"RESIDENCY_ENDED", "DECEASED", "ADMISSION_CANCELLED"})
    void FR_003_finalStatus_isNotChecked(ResidentStatus status) {
        assertThat(Dbr02Check.violations(status, List.of())).isEmpty();
    }
}
