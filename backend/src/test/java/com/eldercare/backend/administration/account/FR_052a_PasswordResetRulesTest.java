package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.domain.PasswordResetRules;
import com.eldercare.backend.support.TableRows;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 002 T105 (FR-052a, BR-M15-25; CFG-M15-13, 14): ai nhận mã, giới hạn xin mã, hủy mã khi sai đủ số lần. */
class FR_052a_PasswordResetRulesTest {

    @Test
    void FR_052a_eligible_onlyStaffOrFamily_activeOrTemporarilyLocked_withProfilePhone() {
        TableRows.csv(
                row -> assertThat(PasswordResetRules.eligible(
                                AccountType.valueOf(row[0]),
                                AccountStatus.valueOf(row[1]),
                                Optional.ofNullable(row[2])))
                        .isEqualTo(Boolean.parseBoolean(row[3])),
                "STAFF,         ACTIVE,             0912000111, true",
                "FAMILY_MEMBER, TEMPORARILY_LOCKED, 0912000111, true",
                "STAFF,         LOCKED,             0912000111, false",
                "FAMILY_MEMBER, INACTIVE,           0912000111, false",
                "RESIDENT,      ACTIVE,             0912000111, false",
                "STAFF,         ACTIVE,             ,           false");
    }

    @Test
    void BR_73_requestLimit_perDayAndInterval_codeRevokedAfterMaxFailures() {
        Instant now = Instant.parse("2026-11-08T02:00:00Z");
        Duration interval = Duration.ofSeconds(60);
        assertThat(PasswordResetRules.canRequest(now, 0, Optional.empty(), 5, interval))
                .isTrue();
        assertThat(PasswordResetRules.canRequest(now, 4, Optional.of(now.minusSeconds(60)), 5, interval))
                .isTrue();
        assertThat(PasswordResetRules.canRequest(now, 4, Optional.of(now.minusSeconds(59)), 5, interval))
                .isFalse();
        assertThat(PasswordResetRules.canRequest(now, 5, Optional.of(now.minusSeconds(3600)), 5, interval))
                .isFalse();
        assertThat(PasswordResetRules.revokeAfter(4, 5)).isFalse();
        assertThat(PasswordResetRules.revokeAfter(5, 5)).isTrue();
    }
}
