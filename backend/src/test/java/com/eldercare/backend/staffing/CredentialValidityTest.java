package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.credential.internal.domain.CredentialValidity;
import com.eldercare.backend.staffing.credential.internal.domain.CredentialValidity.Status;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** T005: hiệu lực dùng ngày nghiệp vụ Việt Nam; một bản phải phủ toàn bộ khoảng đóng. */
class CredentialValidityTest {
    private final BusinessTime time = new BusinessTime(Clock.systemUTC());
    private final LocalDate issue = LocalDate.of(2026, 10, 1);
    private final LocalDate expiry = LocalDate.of(2026, 10, 23);

    @Test
    void DBR_25_expiryIncludesTheWholeVietnameseCalendarDay() {
        assertThat(CredentialValidity.status(
                        issue, expiry, null, false, Instant.parse("2026-10-23T16:59:59.999999Z"), time))
                .isEqualTo(Status.VALID);
        assertThat(CredentialValidity.status(issue, expiry, null, false, Instant.parse("2026-10-23T17:00:00Z"), time))
                .isEqualTo(Status.EXPIRED);
    }

    @Test
    void FR_007_issueDateStartsAtMidnightAndNoExpiryRemainsValid() {
        assertThat(CredentialValidity.status(
                        issue, null, null, false, time.startOfDay(issue).minusNanos(1), time))
                .isEqualTo(Status.NOT_YET_ISSUED);
        assertThat(CredentialValidity.status(issue, null, null, false, time.startOfDay(issue), time))
                .isEqualTo(Status.VALID);
        assertThat(CredentialValidity.status(issue, null, null, false, time.startOfDay(issue.plusYears(10)), time))
                .isEqualTo(Status.VALID);
    }

    @Test
    void FR_007_revocationStartsAtMidnightAndReplacementInvalidatesOldRecord() {
        assertThat(CredentialValidity.status(
                        issue, expiry, expiry, false, time.startOfDay(expiry).minusNanos(1), time))
                .isEqualTo(Status.VALID);
        assertThat(CredentialValidity.status(issue, expiry, expiry, false, time.startOfDay(expiry), time))
                .isEqualTo(Status.REVOKED);
        assertThat(CredentialValidity.status(issue, expiry, null, true, time.startOfDay(issue), time))
                .isEqualTo(Status.REPLACED);
    }

    @Test
    void BR_M09_01_oneCredentialMustCoverBothInclusiveIntervalEndpoints() {
        assertThat(CredentialValidity.covers(
                        issue,
                        expiry,
                        null,
                        false,
                        time.startOfDay(issue),
                        time.startOfDay(expiry.plusDays(1)).minusNanos(1),
                        time))
                .isTrue();
        assertThat(CredentialValidity.covers(
                        issue, expiry, null, false, time.startOfDay(expiry), time.startOfDay(expiry.plusDays(1)), time))
                .isFalse();
        assertThat(CredentialValidity.covers(
                        issue,
                        expiry,
                        null,
                        false,
                        time.startOfDay(issue),
                        time.startOfDay(issue).minusNanos(1),
                        time))
                .isFalse();
    }
}
