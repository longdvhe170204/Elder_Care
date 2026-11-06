package com.eldercare.backend.family.internal.visit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.family.internal.visit.domain.VisitRegistrationCheck.Facts;
import com.eldercare.backend.family.internal.visit.domain.VisitRegistrationCheck.Reason;
import com.eldercare.backend.family.internal.visit.domain.VisitRegistrationCheck.Request;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 012 T022 ({@code KiemTraDangKyThamTest}; research F-08; FR-029, FR-030 (a) → (e), BR-M10-02): mọi lý do từ chối trả
 * cùng lúc; tại quầy cho khung hiện tại không áp (e). Khung mặc định CFG-M10-04 09:00–11:00, 15:00–17:00, sức chứa 20;
 * CFG-M10-05 trước ít nhất 2 giờ, tối đa 14 ngày; CFG-M10-06 3 người.
 */
class VisitRegistrationCheckTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);
    private static final List<VisitSlot> SLOTS = List.of(
            new VisitSlot(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            new VisitSlot(LocalTime.of(15, 0), LocalTime.of(17, 0)));
    private static final VisitSlot MORNING = SLOTS.get(0);

    /** Ngày trước, 08:00 giờ Việt Nam. */
    private static final java.time.Instant DAY_BEFORE = ZonedDateTime.of(
                    DAY.minusDays(1), LocalTime.of(8, 0), VisitRegistrationCheck.ZONE)
            .toInstant();

    private static Facts facts(int reserved, boolean isolated, boolean present, java.time.Instant now) {
        return new Facts(SLOTS, 20, reserved, 3, Duration.ofHours(2), Duration.ofDays(14), isolated, present, now);
    }

    private static Request portal(VisitSlot slot, int visitors) {
        return new Request(DAY, slot.start(), slot.end(), visitors, false);
    }

    @Test
    void FR_031_allConditionsMet_noReason() {
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 3), facts(16, false, true, DAY_BEFORE)))
                .isEmpty();
    }

    @Test
    void FR_030a_outsideVisitSlot() {
        VisitSlot noon = new VisitSlot(LocalTime.of(12, 0), LocalTime.of(13, 0));
        assertThat(VisitRegistrationCheck.check(portal(noon, 1), facts(0, false, true, DAY_BEFORE)))
                .containsExactly(Reason.OUTSIDE_VISIT_SLOT);
    }

    @Test
    void FR_030b_capacityCountsEveryVisitor_lastSeatsExact() {
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 3), facts(17, false, true, DAY_BEFORE)))
                .isEmpty();
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 2), facts(19, false, true, DAY_BEFORE)))
                .containsExactly(Reason.VISIT_SLOT_FULL);
    }

    @Test
    void FR_030c_isolation_and_d_notPresent() {
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 1), facts(0, true, false, DAY_BEFORE)))
                .containsExactly(Reason.ISOLATION_ACTIVE, Reason.RESIDENT_NOT_PRESENT);
    }

    @Test
    void FR_029_tooManyVisitors() {
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 4), facts(0, false, true, DAY_BEFORE)))
                .containsExactly(Reason.TOO_MANY_VISITORS);
    }

    @Test
    void FR_030e_registrationWindow_minAndMaxLead() {
        java.time.Instant oneHourBefore = ZonedDateTime.of(DAY, LocalTime.of(8, 0), VisitRegistrationCheck.ZONE)
                .toInstant();
        java.time.Instant twoHoursBefore = ZonedDateTime.of(DAY, LocalTime.of(7, 0), VisitRegistrationCheck.ZONE)
                .toInstant();
        java.time.Instant fifteenDaysBefore = DAY_BEFORE.minus(Duration.ofDays(14));
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 1), facts(0, false, true, oneHourBefore)))
                .containsExactly(Reason.OUTSIDE_REGISTRATION_WINDOW);
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 1), facts(0, false, true, twoHoursBefore)))
                .isEmpty();
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 1), facts(0, false, true, fifteenDaysBefore)))
                .containsExactly(Reason.OUTSIDE_REGISTRATION_WINDOW);
    }

    @Test
    void FR_030e_counterForCurrentSlot_skipsWindow_butNotForEndedSlot() {
        java.time.Instant during = ZonedDateTime.of(DAY, LocalTime.of(9, 30), VisitRegistrationCheck.ZONE)
                .toInstant();
        java.time.Instant after = ZonedDateTime.of(DAY, LocalTime.of(11, 0), VisitRegistrationCheck.ZONE)
                .toInstant();
        Request counter = new Request(DAY, MORNING.start(), MORNING.end(), 2, true);
        assertThat(VisitRegistrationCheck.check(counter, facts(0, false, true, during)))
                .isEmpty();
        assertThat(VisitRegistrationCheck.check(portal(MORNING, 2), facts(0, false, true, during)))
                .containsExactly(Reason.OUTSIDE_REGISTRATION_WINDOW);
        assertThat(VisitRegistrationCheck.check(counter, facts(0, false, true, after)))
                .containsExactly(Reason.OUTSIDE_REGISTRATION_WINDOW);
    }

    @Test
    void FR_030_everyReasonReturnedTogether() {
        VisitSlot noon = new VisitSlot(LocalTime.of(12, 0), LocalTime.of(13, 0));
        java.time.Instant late = ZonedDateTime.of(DAY, LocalTime.of(11, 30), VisitRegistrationCheck.ZONE)
                .toInstant();
        assertThat(VisitRegistrationCheck.check(portal(noon, 5), facts(20, true, false, late)))
                .containsExactly(
                        Reason.OUTSIDE_VISIT_SLOT,
                        Reason.ISOLATION_ACTIVE,
                        Reason.RESIDENT_NOT_PRESENT,
                        Reason.TOO_MANY_VISITORS,
                        Reason.OUTSIDE_REGISTRATION_WINDOW);
    }

    @Test
    void FR_030_slotLabel_roundTrips() {
        assertThat(VisitSlot.parse(SLOTS.get(1).label())).isEqualTo(SLOTS.get(1));
        assertThat(SLOTS.get(1).label()).isEqualTo("15:00-17:00");
    }
}
