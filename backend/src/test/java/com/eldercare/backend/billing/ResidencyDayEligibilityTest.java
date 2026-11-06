package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.pricing.domain.BillingPeriod;
import com.eldercare.backend.billing.internal.source.domain.ResidencyDayEligibility;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** T009/FR-007/011: điều kiện thuần không cần metadata hoặc producer của module nguồn. */
class ResidencyDayEligibilityTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final BillingPeriod period = BillingPeriod.residential(
            TODAY.minusDays(5), TODAY.minusDays(2), TODAY.plusDays(4), null, null);

    @Test
    void FR_007_anEndedDayBeforeAdmissionRemainsBillableFromTheContractStart() {
        var result = ResidencyDayEligibility.evaluate(period, TODAY.minusDays(4), false, TODAY, false, null, ZONE);
        assertThat(result.allowed()).isTrue();
        assertThat(result.createdInAdvance()).isFalse();
    }

    @Test
    void Q_395_noResidencyFeeIsCreatedFromTheBedReleaseDay() {
        var result = ResidencyDayEligibility.evaluate(period, TODAY.minusDays(1), true, TODAY, false, null, ZONE);
        assertThat(result.allowed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("CONDITION_NOT_MET");
    }

    @Test
    void FR_007_todayHasNotEndedForARegularStatement() {
        var result = ResidencyDayEligibility.evaluate(period, TODAY, false, TODAY, false, null, ZONE);
        assertThat(result.errorCode()).isEqualTo("DAY_NOT_ENDED");
        assertThat(result.createdInAdvance()).isFalse();
    }

    @Test
    void FR_007_aFutureDayIsNotAllowedOnARegularStatement() {
        var result = ResidencyDayEligibility.evaluate(period, TODAY.plusDays(1), false, TODAY, false, null, ZONE);
        assertThat(result.errorCode()).isEqualTo("DAY_NOT_ENDED");
    }

    @Test
    void FR_034_aFinalStatementAllowsTodayThroughItsExpectedEndDateAndMarksAdvance() {
        var through = TODAY.plusDays(2);
        for (var day = TODAY; !day.isAfter(through); day = day.plusDays(1)) {
            var result = ResidencyDayEligibility.evaluate(period, day, false, TODAY, true, through, ZONE);
            assertThat(result.allowed()).isTrue();
            assertThat(result.createdInAdvance()).isTrue();
        }
    }

    @Test
    void FR_034_aFinalStatementDoesNotAllowAdvanceBeyondItsExpectedEndDate() {
        var result = ResidencyDayEligibility.evaluate(period, TODAY.plusDays(2), false,
                TODAY, true, TODAY.plusDays(1), ZONE);
        assertThat(result.errorCode()).isEqualTo("DAY_NOT_ENDED");
    }

    @Test
    void FR_034_aMissingFinalEndDateDoesNotGrantAnUnlimitedAdvanceException() {
        var result = ResidencyDayEligibility.evaluate(period, TODAY, false, TODAY, true, null, ZONE);
        assertThat(result.errorCode()).isEqualTo("DAY_NOT_ENDED");
    }

    @Test
    void FR_011_outsideTheBillingPeriodIsRejectedBeforeTheAdvanceException() {
        var before = ResidencyDayEligibility.evaluate(period, TODAY.minusDays(6), false,
                TODAY, true, TODAY.plusDays(10), ZONE);
        var after = ResidencyDayEligibility.evaluate(period, TODAY.plusDays(5), false,
                TODAY, true, TODAY.plusDays(10), ZONE);
        assertThat(before.errorCode()).isEqualTo("OUTSIDE_BILLING_PERIOD");
        assertThat(after.errorCode()).isEqualTo("OUTSIDE_BILLING_PERIOD");
    }

    @Test
    void FR_036_theDeathDayRemainsBillableButTheFollowingDayDoesNot() {
        var died = BillingPeriod.residential(TODAY.minusDays(5), null, null, null,
                Instant.parse("2026-11-05T01:00:00Z"));
        assertThat(ResidencyDayEligibility.evaluate(died, TODAY.minusDays(1), false,
                TODAY, false, null, ZONE).allowed()).isTrue();
        assertThat(ResidencyDayEligibility.evaluate(died, TODAY, false,
                TODAY, true, TODAY.plusDays(1), ZONE).errorCode()).isEqualTo("OUTSIDE_BILLING_PERIOD");
    }
}
