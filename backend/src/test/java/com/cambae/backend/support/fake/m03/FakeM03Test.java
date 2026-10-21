package com.cambae.backend.support.fake.m03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m03.api.dto.CareLevel;
import com.cambae.backend.m03.api.dto.DayCareSession;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.dto.ResidencyType;
import com.cambae.backend.m03.api.dto.SessionDate;
import com.cambae.backend.m03.api.spi.AllocationInfectionStatus;
import com.cambae.backend.m03.api.spi.AllocationInfectionStatus.RestrictedZone;
import com.cambae.backend.m03.api.spi.AllocationResidencySource;
import com.cambae.backend.m03.api.spi.AllocationResidencySource.BedRetention;
import com.cambae.backend.m03.api.spi.DayCareAttendanceCounter;
import com.cambae.backend.m03.api.spi.LocationReferences;
import com.cambae.backend.m03.api.spi.LocationReferences.ReferenceView;
import com.cambae.backend.m03.internal.bed.domain.port.BedAssetPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** 003 T002, G-16: dữ liệu bản giả do test đặt, không truy cập module khác hay cơ sở dữ liệu. */
class FakeM03Test {

    private static final Instant AT = Instant.parse("2026-10-22T03:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 22);

    @Test
    void FR_018_missingResidencyData_staysMissingUntilConfigured() {
        FakeAllocationResidencies fake = new FakeAllocationResidencies();
        assertThat(fake.residencyType(1L)).isEmpty();
        assertThat(fake.contractCareLevel(1L)).isEmpty();
        assertThat(fake.registrationCareLevel(1L)).isEmpty();
        assertThat(fake.expectedEnd(1L)).isEmpty();
        assertThat(fake.bedRetentionWhenAbsent(1L)).isEqualTo(new BedRetention(false, Optional.empty()));

        BedRetention retention = new BedRetention(true, Optional.of(AT));
        fake.residencyType(1L, ResidencyType.SHORT_TERM)
                .contractCareLevel(1L, CareLevel.SPECIAL)
                .registrationCareLevel(1L, CareLevel.BASIC)
                .expectedEnd(1L, AT)
                .bedRetention(1L, retention);

        assertThat(fake.residencyType(1L)).contains(ResidencyType.SHORT_TERM);
        assertThat(fake.contractCareLevel(1L)).contains(CareLevel.SPECIAL);
        assertThat(fake.registrationCareLevel(1L)).contains(CareLevel.BASIC);
        assertThat(fake.expectedEnd(1L)).contains(AT);
        assertThat(fake.bedRetentionWhenAbsent(1L)).isEqualTo(retention);
        assertThat(fake.residencyType(2L)).isEmpty();
        fake.reset();
        assertThat(fake.contractCareLevel(1L)).isEmpty();
        assertThat(fake.registrationCareLevel(1L)).isEmpty();
        assertThat(fake.expectedEnd(1L)).isEmpty();
        assertThat(fake.bedRetentionWhenAbsent(1L).retained()).isFalse();
    }

    @Test
    void FR_030_priceDifference_isScopedToResidentRoomsAndDate() {
        FakeAllocationResidencies fake = new FakeAllocationResidencies();
        fake.differentUnitPrice(1L, 2L, 3L, DATE, true);
        assertThat(fake.differentUnitPrice(1L, 2L, 3L, DATE)).isTrue();
        assertThat(fake.differentUnitPrice(2L, 2L, 3L, DATE)).isFalse();
        assertThat(fake.differentUnitPrice(1L, 3L, 2L, DATE)).isFalse();
        assertThat(fake.differentUnitPrice(1L, 2L, 3L, DATE.plusDays(1))).isFalse();
        fake.reset();
        assertThat(fake.differentUnitPrice(1L, 2L, 3L, DATE)).isFalse();
    }

    @Test
    void FR_015_residencyCallbacks_areRecordedForIntegrationAssertions() {
        FakeAllocationResidencies fake = new FakeAllocationResidencies();
        fake.waitingFileHoldExpired(11L);
        fake.allocated(1L, 21L);
        assertThat(fake.expiredWaitingFiles()).containsExactly(11L);
        assertThat(fake.allocations()).containsExactly(new FakeAllocationResidencies.Allocation(1L, 21L));
        fake.reset();
        assertThat(fake.expiredWaitingFiles()).isEmpty();
        assertThat(fake.allocations()).isEmpty();
    }

    @Test
    void FR_040_attendanceCount_filtersByDateAndRequestedSessions() {
        FakeDayCareAttendances fake = new FakeDayCareAttendances();
        DayCareSession morning = new DayCareSession(1L, "Buổi sáng", LocalTime.of(7, 0), LocalTime.NOON);
        fake.count(new SessionDate(DATE.minusDays(1), 1L), 9)
                .count(new SessionDate(DATE, 1L), 3)
                .count(new SessionDate(DATE, 2L), 2);
        assertThat(fake.countFrom(DATE, List.of(morning)))
                .containsExactlyEntriesOf(Map.of(new SessionDate(DATE, 1L), 3));
        assertThat(fake.countFrom(DATE, List.of())).isEmpty();
        assertThat(fake.hasFutureSchedule(DATE)).isTrue();
        assertThat(fake.hasFutureSchedule(DATE.plusDays(1))).isFalse();
        fake.reset();
        assertThat(fake.countFrom(DATE, List.of(morning))).isEmpty();
        assertThat(fake.hasFutureSchedule(DATE)).isFalse();
    }

    @Test
    void FR_009_infectionData_respectsTheRequestedInstantAndReset() {
        FakeInfectionStatuses fake = new FakeInfectionStatuses();
        Set<Long> floors = new HashSet<>(Set.of(2L));
        RestrictedZone zone = new RestrictedZone(floors, Set.of(3L));
        fake.zone(AT, zone).listed(1L, AT, true).listed(1L, AT.plusSeconds(60), false);
        floors.clear();
        assertThat(fake.restrictedZone(AT.minusSeconds(1)).floorIds()).isEmpty();
        assertThat(fake.restrictedZone(AT).floorIds()).containsExactly(2L);
        assertThat(fake.restrictedZone(AT).areaIds()).containsExactly(3L);
        assertThat(fake.listed(1L, AT.minusSeconds(1))).isFalse();
        assertThat(fake.listed(1L, AT)).isTrue();
        assertThat(fake.listed(1L, AT.plusSeconds(60))).isFalse();
        assertThat(fake.listed(2L, AT)).isFalse();
        fake.reset();
        assertThat(fake.restrictedZone(AT).floorIds()).isEmpty();
        assertThat(fake.listed(1L, AT)).isFalse();
    }

    @Test
    void FR_005_historicalReferences_remainWhenActiveReferencesAreRemoved() {
        FakeLocationReferences fake = new FakeLocationReferences();
        LocationRef floor = new LocationRef(LocationRef.Type.FLOOR, 2L);
        assertThat(fake.activeReferences(floor)).isEmpty();
        assertThat(fake.everReferenced(floor)).isFalse();
        List<ReferenceView> references = new ArrayList<>(List.of(new ReferenceView("ASSIGNMENT", 3L, "Phân công")));
        fake.active(floor, references);
        references.clear();
        assertThat(fake.activeReferences(floor)).hasSize(1);
        assertThat(fake.everReferenced(floor)).isTrue();
        fake.active(floor, List.of());
        assertThat(fake.activeReferences(floor)).isEmpty();
        assertThat(fake.everReferenced(floor)).isTrue();
        LocationRef oldRoom = new LocationRef(LocationRef.Type.ROOM, 4L);
        fake.historical(oldRoom);
        assertThat(fake.activeReferences(oldRoom)).isEmpty();
        assertThat(fake.everReferenced(oldRoom)).isTrue();
        fake.reset();
        assertThat(fake.everReferenced(floor)).isFalse();
        assertThat(fake.everReferenced(oldRoom)).isFalse();
    }

    @Test
    void Q_238_bedAssets_createIsNoOp_andFlagsAreExplicit() {
        FakeBedAssets fake = new FakeBedAssets();
        fake.createForBed(1L);
        assertThat(fake.pendingResidentMove(1L)).isFalse();
        fake.pendingResidentMove(2L, true).pendingResidentMove(3L, true);
        assertThat(fake.flaggedBeds(List.of(1L, 2L, 4L))).containsExactly(2L);
        fake.pendingResidentMove(2L, false);
        assertThat(fake.pendingResidentMove(2L)).isFalse();
        assertThat(fake.pendingResidentMove(3L)).isTrue();
        fake.reset();
        assertThat(fake.flaggedBeds(List.of(2L, 3L))).isEmpty();
    }

    @Test
    void Q_238_bedAssetFailure_canBeConfiguredForRollbackTests() {
        FakeBedAssets fake = new FakeBedAssets();
        fake.failCreation(true);
        assertThatThrownBy(() -> fake.createForBed(1L)).isInstanceOf(IllegalStateException.class);
        fake.reset();
        fake.createForBed(1L);
    }

    @Test
    void FR_018_testConfiguration_exposesAllFiveFakesThroughTheirInterfaces() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FakeM03Config.class)) {
            assertThat(context.getBean(AllocationResidencySource.class)).isInstanceOf(FakeAllocationResidencies.class);
            assertThat(context.getBean(DayCareAttendanceCounter.class)).isInstanceOf(FakeDayCareAttendances.class);
            assertThat(context.getBean(AllocationInfectionStatus.class)).isInstanceOf(FakeInfectionStatuses.class);
            assertThat(context.getBean(LocationReferences.class)).isInstanceOf(FakeLocationReferences.class);
            assertThat(context.getBean(BedAssetPort.class)).isInstanceOf(FakeBedAssets.class);
        }
    }
}
