package com.cambae.backend.support.fake.m15;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.AssignmentKind;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.BedAllocationView;
import com.cambae.backend.m15.account.api.LicenseValidity;
import com.cambae.backend.m15.account.api.ScopeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 002 T003: bản giả các SPI 002 khai báo cho module khác cài. */
class FakeM15Test {

    private static final Instant T = Instant.parse("2026-10-20T01:00:00Z");

    @Test
    void FR_038_license_validThroughExpiryDay() {
        FakeLicenses licenses = new FakeLicenses()
                .license(7, "PRACTICE", new LicenseValidity(1, "CCHN", LocalDate.of(2026, 10, 20), Set.of()));

        assertThat(licenses.valid(7, "PRACTICE", LocalDate.of(2026, 10, 20))).isPresent();
        assertThat(licenses.valid(7, "PRACTICE", LocalDate.of(2026, 10, 21))).isEmpty();
        assertThat(licenses.valid(8, "PRACTICE", LocalDate.of(2026, 10, 20))).isEmpty();
    }

    @Test
    void FR_032_assignment_activeWithinWindow() {
        AssignmentView shift =
                new AssignmentView(AssignmentKind.SHIFT_MEMBER, 10L, null, null, 3L, T, T.plusSeconds(3600));
        FakeAssignments assignments = new FakeAssignments().assign(7, shift);

        assertThat(assignments.activeAt(7, T)).containsExactly(shift);
        assertThat(assignments.activeAt(7, T.plusSeconds(3600))).isEmpty();
        assertThat(assignments.history(7, T.minusSeconds(60), T)).isEmpty();
        assertThat(assignments.history(7, T.minusSeconds(60), T.plusSeconds(1))).containsExactly(shift);
    }

    @Test
    void FR_029_bedAllocation_residentsInFloorOrRoom() {
        FakeBedAllocations beds = new FakeBedAllocations()
                .allocate(100, new BedAllocationView(1, 11, 21, 10))
                .allocate(101, new BedAllocationView(2, 12, 22, 20));

        assertThat(beds.residentsIn(new ScopeFilter(false, Set.of(10L), Set.of(), Set.of()), T))
                .containsExactly(100L);
        assertThat(beds.residentsIn(new ScopeFilter(false, Set.of(), Set.of(22L), Set.of()), T))
                .containsExactly(101L);
        assertThat(beds.residentsIn(ScopeFilter.FACILITY, T)).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void FR_045_familyRelationship_endedIsNotActive() {
        FakeFamilyRelationships family = new FakeFamilyRelationships();
        long relationship = family.relate(5, 100);
        family.representative(relationship);

        assertThat(family.belongsToResident(5, 100)).isTrue();
        assertThat(family.isActiveRepresentative(5, 100, T)).isTrue();

        family.end(relationship);

        assertThat(family.belongsToResident(5, 100)).isFalse();
        assertThat(family.active(5)).isEmpty();
    }
}
