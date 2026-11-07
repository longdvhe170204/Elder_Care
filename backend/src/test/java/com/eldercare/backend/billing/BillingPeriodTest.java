package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.pricing.domain.BillingPeriod;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** T006: khoảng tính phí thuần, ngày nghiệp vụ do bên gọi lấy từ nguồn 004/001. */
class BillingPeriodTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate ADMITTED = START.plusDays(5);

    private static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(ZONE).toInstant();
    }

    @Test
    void Q_22_residentialStartsAtTheContractDateEvenWhenAdmissionIsLater() {
        var period = BillingPeriod.residential(START, ADMITTED, null, null, null);
        assertThat(period.startsOn()).isEqualTo(START);
        assertThat(period.contains(at("2026-10-05T00:00:00"), ZONE)).isTrue();
        assertThat(period.notYetAdmitted(START)).isTrue();
        assertThat(period.notYetAdmitted(ADMITTED.minusDays(1))).isTrue();
        assertThat(period.notYetAdmitted(ADMITTED)).isFalse();
        assertThat(period.contains(at("2026-10-04T23:59:59.999999"), ZONE)).isFalse();
    }

    @Test
    void Q_22_noCompletedAdmissionKeepsTheNotYetAdmittedFlag() {
        var period = BillingPeriod.residential(START, null, null, null, null);
        assertThat(period.contains(at("2026-10-06T12:00:00"), ZONE)).isTrue();
        assertThat(period.notYetAdmitted(START.plusDays(1))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "2026-10-04T23:59:59.999999,false",
        "2026-10-05T00:00:00,true",
        "2026-10-20T23:59:59.999999,true",
        "2026-10-21T00:00:00,false"
    })
    void FR_011_expectedEndIncludesItsWholeBusinessDay(String local, boolean included) {
        var period = BillingPeriod.residential(START, ADMITTED, LocalDate.of(2026, 10, 20), null, null);
        assertThat(period.contains(at(local), ZONE)).isEqualTo(included);
    }

    @Test
    void Q_27_cancellationKeepsTheWholeCancellationDay() {
        var cancellation = LocalDate.of(2026, 10, 15);
        var period = BillingPeriod.residential(START, ADMITTED, null, cancellation, null);
        assertThat(period.contains(at("2026-10-15T23:59:59.999999"), ZONE)).isTrue();
        assertThat(period.contains(at("2026-10-16T00:00:00"), ZONE)).isFalse();
    }

    @Test
    void FR_011_theEarlierOfCancellationAndExpectedEndLimitsThePeriod() {
        var period = BillingPeriod.residential(
                START, ADMITTED, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 15), null);
        assertThat(period.contains(at("2026-10-16T08:00:00"), ZONE)).isFalse();
        var earlierEnd = BillingPeriod.residential(
                START, ADMITTED, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 15), null);
        assertThat(earlierEnd.contains(at("2026-10-13T08:00:00"), ZONE)).isFalse();
    }

    @Test
    void FR_011_eventsAfterDeathAreOutsideEvenOnTheSameDay() {
        var death = at("2026-10-15T10:30:00");
        var period = BillingPeriod.residential(START, ADMITTED, LocalDate.of(2026, 10, 20), null, death);
        assertThat(period.contains(death.minusNanos(1), ZONE)).isTrue();
        assertThat(period.contains(death, ZONE)).isTrue();
        assertThat(period.contains(death.plusNanos(1), ZONE)).isFalse();
    }

    @Test
    void FR_036_residencyFeeStillIncludesTheDeathDay() {
        var death = at("2026-10-15T10:30:00");
        var period = BillingPeriod.residential(START, ADMITTED, null, null, death);
        assertThat(period.containsDay(LocalDate.of(2026, 10, 15), ZONE)).isTrue();
        assertThat(period.containsDay(LocalDate.of(2026, 10, 16), ZONE)).isFalse();
        assertThat(period.contains(at("2026-10-15T11:00:00"), ZONE)).isFalse();
    }

    @Test
    void FR_011_dayCareStartsWithTheFirstScheduledSessionFromTheContractStart() {
        var first = LocalDate.of(2026, 10, 8);
        var sessionStarts = at("2026-10-08T08:00:00");
        var period = BillingPeriod.dayCare(START, sessionStarts, null, null, null, ZONE)
                .orElseThrow();
        assertThat(period.startsOn()).isEqualTo(first);
        assertThat(period.contains(sessionStarts.minusNanos(1), ZONE)).isFalse();
        assertThat(period.contains(sessionStarts, ZONE)).isTrue();
        assertThat(period.containsDay(first, ZONE)).isTrue();
        assertThat(period.notYetAdmitted(first)).isFalse();
    }

    @Test
    void FR_011_dayCareWithoutAScheduledSessionHasNoBillingPeriod() {
        assertThat(BillingPeriod.dayCare(START, null, null, null, null, ZONE)).isEmpty();
    }

    @Test
    void FR_011_sessionBeforeTheContractStartIsInvalidSourceData() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BillingPeriod.dayCare(START, at("2026-10-04T08:00:00"), null, null, null, ZONE));
    }

    @Test
    void FR_011_movingOrRemovingTheExpectedEndMakesTheSourceEligibleAgain() {
        var source = at("2026-10-21T08:00:00");
        assertThat(BillingPeriod.residential(START, ADMITTED, LocalDate.of(2026, 10, 20), null, null)
                        .contains(source, ZONE))
                .isFalse();
        assertThat(BillingPeriod.residential(START, ADMITTED, LocalDate.of(2026, 10, 22), null, null)
                        .contains(source, ZONE))
                .isTrue();
        assertThat(BillingPeriod.residential(START, ADMITTED, null, null, null).contains(source, ZONE))
                .isTrue();
    }

    @Test
    void FR_011_anEndingBeforeTheContractStartLeavesNoEligibleEvent() {
        var period = BillingPeriod.residential(START, null, START.minusDays(1), null, null);
        assertThat(period.contains(at("2026-10-05T00:00:00"), ZONE)).isFalse();
    }
}
