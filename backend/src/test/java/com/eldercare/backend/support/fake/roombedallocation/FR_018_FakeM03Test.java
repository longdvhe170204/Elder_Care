package com.eldercare.backend.support.fake.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.eldercare.backend.roombedallocation.api.dto.DayCareSession;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.api.spi.CordonedArea;
import com.eldercare.backend.roombedallocation.api.spi.DayCareArrivalCounter;
import com.eldercare.backend.roombedallocation.api.spi.HeldBed;
import com.eldercare.backend.roombedallocation.api.spi.LocationReferenceView;
import com.eldercare.backend.roombedallocation.api.spi.LocationReferences;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** T002, G-16: bản giả chỉ trả dữ liệu test đặt, không cần dịch vụ hay cơ sở dữ liệu của module khác. */
class FR_018_FakeM03Test {

    private static final Instant AT = Instant.parse("2026-10-22T01:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

    @Test
    void FR_018_defaultsContainNoResidentNoInfectionAndNoAssetFlag() {
        FakeAllocationResidencySource residency = new FakeAllocationResidencySource();
        assertThat(residency.residencyType(1)).isEmpty();
        assertThat(residency.careLevelFromContract(1)).isEmpty();
        assertThat(residency.careLevelFromRegistration(1)).isEmpty();
        assertThat(residency.expectedEnd(1)).isEmpty();
        assertThat(residency.holdBedWhenAbsent(1)).isEqualTo(new HeldBed(false, Optional.empty()));
        assertThat(residency.differentUnitPrice(1, 2, 3, DAY)).isFalse();
        assertThat(new FakeAllocationInfectionStatus().cordonedAt(AT)).isEqualTo(new CordonedArea(Set.of(), Set.of()));
        assertThat(new FakeAllocationInfectionStatus().listedAt(1, AT)).isFalse();
        FakeBedAssets assets = new FakeBedAssets();
        assertThatCode(() -> assets.recordIncreaseForBed(1)).doesNotThrowAnyException();
        assertThat(assets.awaitingResidentTransfer(1)).isFalse();
        assertThat(assets.flaggedBeds(List.of(1L, 2L))).isEmpty();
    }

    @Test
    void FR_018_residencyFixturesAndCallbacksAreIsolatedAndResettable() {
        FakeAllocationResidencySource fake = new FakeAllocationResidencySource();
        HeldBed held = new HeldBed(true, Optional.of(AT));
        fake.resident(1, ResidencyType.SHORT_TERM, CareLevelCode.SPECIAL, CareLevelCode.BASIC, AT, held);
        fake.priceDifference(1, 2, 3, DAY, true);
        assertThat(fake.residencyType(1)).contains(ResidencyType.SHORT_TERM);
        assertThat(fake.careLevelFromContract(1)).contains(CareLevelCode.SPECIAL);
        assertThat(fake.careLevelFromRegistration(1)).contains(CareLevelCode.BASIC);
        assertThat(fake.expectedEnd(1)).contains(AT);
        assertThat(fake.holdBedWhenAbsent(1)).isEqualTo(held);
        assertThat(fake.differentUnitPrice(1, 2, 3, DAY)).isTrue();
        assertThat(fake.differentUnitPrice(9, 2, 3, DAY)).isFalse();
        fake.waitingRecordHoldExpired(7);
        fake.allocated(1, 8);
        assertThat(fake.expiredWaitingRecords()).containsExactly(7L);
        assertThat(fake.allocations()).containsEntry(1L, 8L);
        fake.reset();
        assertThat(fake.residencyType(1)).isEmpty();
        assertThat(fake.holdBedWhenAbsent(1).held()).isFalse();
        assertThat(fake.differentUnitPrice(1, 2, 3, DAY)).isFalse();
        assertThat(fake.expiredWaitingRecords()).isEmpty();
        assertThat(fake.allocations()).isEmpty();
    }

    @Test
    void FR_040_dayCareCounterFiltersByDateAndRequestedSessions() {
        FakeDayCareArrivals fake = new FakeDayCareArrivals();
        DayCareSession morning = new DayCareSession(1, "Sáng", LocalTime.of(7, 0), LocalTime.NOON, true);
        SessionDate today = new SessionDate(DAY, 1);
        assertThat(fake.countFrom(DAY, List.of(morning))).isEmpty();
        assertThat(fake.hasFutureArrivals(DAY)).isFalse();
        fake.count(new SessionDate(DAY.minusDays(1), 1), 2).count(today, 3).count(new SessionDate(DAY, 2), 4);
        assertThat(fake.countFrom(DAY, List.of(morning)))
                .containsOnlyKeys(today)
                .containsEntry(today, 3);
        assertThat(fake.hasFutureArrivals(DAY)).isTrue();
        assertThat(fake.hasFutureArrivals(DAY.plusDays(1))).isFalse();
        fake.reset();
        assertThat(fake.hasFutureArrivals(DAY)).isFalse();
    }

    @Test
    void FR_009_infectionFixturesRespectTheRequestedInstant() {
        FakeAllocationInfectionStatus fake = new FakeAllocationInfectionStatus();
        CordonedArea cordon = new CordonedArea(Set.of(1L), Set.of(2L));
        fake.cordon(AT, cordon).listed(3, AT, true).listed(3, AT.plusSeconds(60), false);
        assertThat(fake.cordonedAt(AT.minusSeconds(1)).floorIds()).isEmpty();
        assertThat(fake.cordonedAt(AT)).isEqualTo(cordon);
        assertThat(fake.listedAt(3, AT.minusSeconds(1))).isFalse();
        assertThat(fake.listedAt(3, AT)).isTrue();
        assertThat(fake.listedAt(3, AT.plusSeconds(60))).isFalse();
        fake.reset();
        assertThat(fake.cordonedAt(AT).floorIds()).isEmpty();
        assertThat(fake.listedAt(3, AT)).isFalse();
    }

    @Test
    void FR_005_locationReferencesKeepHistoryAfterCurrentReferencesAreRemoved() {
        FakeLocationReferences fake = new FakeLocationReferences();
        LocationRef floor = new LocationRef("FLOOR", 1);
        LocationReferenceView reference = new LocationReferenceView("ASSIGNMENT", 7, "Phân công ca");
        assertThat(fake.currentReferences(floor)).isEmpty();
        assertThat(fake.everReferenced(floor)).isFalse();
        fake.reference(floor, reference);
        assertThat(fake.currentReferences(floor)).containsExactly(reference);
        fake.clearCurrent(floor);
        assertThat(fake.currentReferences(floor)).isEmpty();
        assertThat(fake.everReferenced(floor)).isTrue();
        fake.reset();
        assertThat(fake.everReferenced(floor)).isFalse();
    }

    @Test
    void FR_018_assetFlagsAreSwitchableAndRestrictedToRequestedBeds() {
        FakeBedAssets fake = new FakeBedAssets();
        fake.flag(1, true).flag(2, true);
        assertThat(fake.awaitingResidentTransfer(1)).isTrue();
        assertThat(fake.flaggedBeds(List.of(1L, 3L))).containsExactly(1L);
        fake.flag(1, false);
        assertThat(fake.awaitingResidentTransfer(1)).isFalse();
        fake.reset();
        assertThat(fake.flaggedBeds(List.of(2L))).isEmpty();
    }

    @Test
    void FR_018_importRegistersExactlyOneFakeForEachDeclaredPort() {
        new ApplicationContextRunner()
                .withUserConfiguration(FakeM03Config.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AllocationResidencySource.class);
                    assertThat(context).hasSingleBean(DayCareArrivalCounter.class);
                    assertThat(context).hasSingleBean(AllocationInfectionStatus.class);
                    assertThat(context).hasSingleBean(LocationReferences.class);
                    assertThat(context).hasSingleBean(BedAssetPort.class);
                    assertThat(context).doesNotHaveBean(javax.sql.DataSource.class);
                });
    }
}
