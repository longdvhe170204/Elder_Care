package com.cambae.backend.support.fake.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import com.cambae.backend.m09.api.spi.HeldBelongingView;
import com.cambae.backend.m09.api.spi.HeldBelongingsHandoverPort;
import com.cambae.backend.m09.api.spi.TripDutyPort;
import com.cambae.backend.m09.api.spi.TripInShiftView;
import com.cambae.backend.m09.shift.internal.service.port.ExtendedPublicationCheck;
import com.cambae.backend.m09.shift.internal.service.port.PublicationWarning;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/** T002: dữ liệu test đặt qua bản giả, không phụ thuộc các bản cài 013, 014, 015 hoặc cơ sở dữ liệu. */
class FakeM09Test {

    private static final Instant FROM = Instant.parse("2026-10-23T00:00:00Z");
    private static final Instant TO = FROM.plusSeconds(8 * 3600);

    @Test
    void FR_039_heldBelongingsAreScopedToRequestedHolders() {
        var fake = new FakeHeldBelongings();
        var first = new HeldBelongingView(1L, 10L, 100L, 1000L);
        var second = new HeldBelongingView(2L, 20L, 200L, null);
        fake.hold(first).hold(second);

        assertThat(fake.heldBy(List.of(10L))).containsExactly(first);
        assertThat(fake.heldBy(List.of(30L))).isEmpty();
        assertThat(fake.heldBy(List.of())).isEmpty();
    }

    @Test
    void FR_040_stillHeldReflectsReleaseHolderChangesAndUnknownIds() {
        var fake = new FakeHeldBelongings();
        fake.hold(new HeldBelongingView(1L, 10L, 100L, null)).hold(new HeldBelongingView(2L, 20L, 200L, null));

        assertThat(fake.stillHeld(List.of(1L, 2L, 3L), List.of(10L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, true, 2L, false, 3L, false));

        fake.hold(new HeldBelongingView(1L, 30L, 100L, null)).release(2L);

        assertThat(fake.stillHeld(List.of(1L, 2L), List.of(10L, 20L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, false, 2L, false));
        assertThat(fake.stillHeld(List.of(), List.of(10L))).isEmpty();
    }

    @Test
    void FR_039_tripDutiesDefaultToEmptyAndKeepTripsSeparate() {
        var fake = new FakeTripDuties();
        fake.groupLeader(1L, 10L).companions(1L, List.of(20L, 30L));

        assertThat(fake.groupLeader(1L)).contains(10L);
        assertThat(fake.companions(1L)).containsExactly(20L, 30L);
        assertThat(fake.groupLeader(2L)).isEmpty();
        assertThat(fake.companions(2L)).isEmpty();
        assertThat(fake.inShift(100L, FROM, TO)).isEmpty();
    }

    @Test
    void FR_039_tripsMatchFloorAndOverlapWithoutIncludingAdjacentIntervals() {
        var fake = new FakeTripDuties();
        var crossing = new TripInShiftView(1L, List.of(10L), TO);
        fake.trip(100L, FROM.minusSeconds(1), FROM.plusSeconds(1), crossing)
                .trip(200L, FROM, TO, new TripInShiftView(2L, List.of(20L), TO))
                .trip(100L, FROM.minusSeconds(3600), FROM, new TripInShiftView(3L, List.of(30L), FROM))
                .trip(100L, TO, TO.plusSeconds(3600), new TripInShiftView(4L, List.of(40L), TO));

        assertThat(fake.inShift(100L, FROM, TO)).containsExactly(crossing);
    }

    @Test
    void FR_039_ongoingTripRemainsVisibleAfterExpectedReturnTime() {
        var fake = new FakeTripDuties();
        var overdue = new TripInShiftView(1L, List.of(10L), FROM.minusSeconds(60));
        fake.trip(100L, FROM.minusSeconds(3600), null, overdue);

        assertThat(fake.inShift(100L, FROM, TO)).containsExactly(overdue);
    }

    @Test
    void FR_039_tripCollectionsAreSnapshots() {
        var staff = new ArrayList<>(List.of(10L));
        var trip = new TripInShiftView(1L, staff, TO);
        var fake = new FakeTripDuties().companions(1L, staff);
        staff.add(20L);

        assertThat(trip.staffIds()).containsExactly(10L);
        assertThat(fake.companions(1L)).containsExactly(10L);
    }

    @Test
    void FR_016_publicationWarningsAreScopedToScheduleAndSnapshotInput() {
        var fake = new FakeExtendedPublicationChecks();
        var warning =
                new PublicationWarning("APPROVED_LEAVE_DAY", "Có yêu cầu nghỉ theo ngày chưa quyết định", Map.of());
        var warnings = new ArrayList<>(List.of(warning));
        fake.warnings(1L, warnings);
        warnings.clear();

        assertThat(fake.check(1L)).containsExactly(warning);
        assertThat(fake.check(2L)).isEmpty();
        fake.warnings(1L, List.of());
        assertThat(fake.check(1L)).isEmpty();
    }

    @Test
    void FR_039_belongingsUnavailableIsDistinctFromAnEmptyResultAndCanRecover() {
        var fake = new FakeHeldBelongings().available(false);

        assertThatIllegalStateException().isThrownBy(() -> fake.heldBy(List.of(10L)));
        assertThatIllegalStateException().isThrownBy(() -> fake.stillHeld(List.of(1L), List.of(10L)));
        fake.available(true);
        assertThat(fake.heldBy(List.of(10L))).isEmpty();
    }

    @Test
    void FR_039_tripsUnavailableIsDistinctFromAnEmptyResultAndCanRecover() {
        var fake = new FakeTripDuties().available(false);

        assertThatIllegalStateException().isThrownBy(() -> fake.groupLeader(1L));
        assertThatIllegalStateException().isThrownBy(() -> fake.companions(1L));
        assertThatIllegalStateException().isThrownBy(() -> fake.inShift(100L, FROM, TO));
        fake.available(true);
        assertThat(fake.groupLeader(1L)).isEmpty();
    }

    @Test
    void FR_016_publicationChecksUnavailableCanRecover() {
        var fake = new FakeExtendedPublicationChecks().available(false);

        assertThatIllegalStateException().isThrownBy(() -> fake.check(1L));
        fake.available(true);
        assertThat(fake.check(1L)).isEmpty();
    }

    @Test
    void FR_039_testConfigurationSelectsFakesEvenWhenOtherImplementationsExist() {
        new ApplicationContextRunner()
                .withUserConfiguration(FakeM09Config.class, OtherImplementations.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HeldBelongingsHandoverPort.class))
                            .isSameAs(context.getBean(FakeHeldBelongings.class));
                    assertThat(context.getBean(TripDutyPort.class)).isSameAs(context.getBean(FakeTripDuties.class));
                    assertThat(context.getBean(ExtendedPublicationCheck.class))
                            .isSameAs(context.getBean(FakeExtendedPublicationChecks.class));
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OtherImplementations {

        @Bean
        HeldBelongingsHandoverPort otherBelongings() {
            return mock(HeldBelongingsHandoverPort.class);
        }

        @Bean
        TripDutyPort otherTrips() {
            return mock(TripDutyPort.class);
        }

        @Bean
        ExtendedPublicationCheck otherPublicationCheck() {
            return mock(ExtendedPublicationCheck.class);
        }
    }
}
