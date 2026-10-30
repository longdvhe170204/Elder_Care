package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.dto.CareShiftView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.dto.AlertShiftView;
import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.support.fake.care.FakeCareShifts;
import com.eldercare.backend.support.fake.incident.FakeAlertStaff;
import com.eldercare.backend.support.fake.medication.FakeNursingShifts;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocationReferences;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T015: bản giả dùng chung của SPI được phép khai báo cho C/D, không dùng trong ứng dụng thật. */
class ShiftPortFakesTest {
    private final Instant from = Instant.parse("2026-11-01T00:00:00Z");
    private final Instant to = from.plusSeconds(3600);

    @Test
    void FR_049_careFakeKeepsInputSnapshotAndUsesHalfOpenIntervals() {
        var fake = new FakeCareShifts();
        var members = new ArrayList<>(List.of(10L));
        fake.shift(new CareShiftView(1, 2L, from, to, true), members, 10L);
        members.clear();
        assertThat(fake.listedInShift(10, from)).isTrue();
        assertThat(fake.shiftAt(2, to)).isEmpty();
        fake.qualification(
                10, Role.NURSING_STAFF, Set.of("LICENSE"), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
        assertThat(fake.hasQualifiedStaff(1, Role.NURSING_STAFF, Set.of("LICENSE")))
                .isTrue();
        assertThat(fake.qualified(10, Set.of("LICENSE"), LocalDate.of(2026, 11, 3)))
                .isFalse();
    }

    @Test
    void FR_049_nursingAndAlertFakesKeepScopeAndMembershipSeparate() {
        var nursing = new FakeNursingShifts().shift(new NursingShiftView(1, 2L, from, to), List.of(10L));
        assertThat(nursing.nursesWithShift(3, from)).isEmpty();
        assertThat(nursing.inProgressShiftMember(10, from)).isTrue();
        var alerts = new FakeAlertStaff().shift(new AlertShiftView(1, 2L, from, to), List.of(10L), List.of(20L));
        assertThat(alerts.floorDuringShift(10, from)).contains(2L);
        assertThat(alerts.floorDuringShift(99, from)).isEmpty();
        assertThat(alerts.onDutyDoctors(to)).isEmpty();
    }

    @Test
    void FR_005_locationFakeRetainsPastReferenceAfterActiveReferenceEnds() {
        var fake = new FakeLocationReferences();
        var location = new LocationRef("FLOOR", 2);
        fake.references(location, List.of(new LocationReferenceView("M09.SHIFT", 1)));
        fake.references(location, List.of());
        assertThat(fake.currentReferences(location)).isEmpty();
        assertThat(fake.everReferenced(location)).isTrue();
        assertThat(fake.everReferenced(new LocationRef("FLOOR", 99))).isFalse();
    }
}
