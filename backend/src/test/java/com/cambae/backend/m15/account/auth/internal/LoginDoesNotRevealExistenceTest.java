package com.cambae.backend.m15.account.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.AccountStatus;
import com.cambae.backend.m15.account.auth.internal.LoginRules.Decision;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 002 T038 (NFR-08; FR-014; research A-03): tên đăng nhập sai, mật khẩu sai, tài khoản khóa mà sai mật khẩu đều nhận cùng
 * một lỗi; chỉ khi mật khẩu đúng mà tài khoản không Hoạt động mới có lỗi riêng (khóa tạm kèm thời điểm mở, hoặc liên hệ
 * viện). Khóa tạm đã hết hạn coi như Hoạt động.
 */
class LoginDoesNotRevealExistenceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");
    private static final Instant LATER = NOW.plus(Duration.ofMinutes(10));

    @Test
    void NFR_08_unknownUser_wrongPassword_lockedWithWrongPassword_sameRefusal() {
        Decision unknown = LoginRules.decide(null, null, false, NOW);
        Decision wrongPassword = LoginRules.decide(AccountStatus.ACTIVE, null, false, NOW);
        Decision lockedWrong = LoginRules.decide(AccountStatus.LOCKED, null, false, NOW);
        Decision tempLockedWrong = LoginRules.decide(AccountStatus.TEMPORARILY_LOCKED, LATER, false, NOW);
        Decision inactiveWrong = LoginRules.decide(AccountStatus.INACTIVE, null, false, NOW);

        assertThat(unknown.errorCode()).isEqualTo("LOGIN_FAILED");
        assertThat(unknown.httpStatus()).isEqualTo(401);
        for (Decision d : new Decision[] {wrongPassword, lockedWrong, tempLockedWrong, inactiveWrong}) {
            assertThat(d.errorCode()).isEqualTo(unknown.errorCode());
            assertThat(d.httpStatus()).isEqualTo(unknown.httpStatus());
            assertThat(d.unlocksAt()).isNull();
            assertThat(d.allowed()).isFalse();
        }
    }

    @Test
    void FR_014_correctPassword_notActive_saysWhy() {
        Decision temp = LoginRules.decide(AccountStatus.TEMPORARILY_LOCKED, LATER, true, NOW);
        assertThat(temp.errorCode()).isEqualTo("ACCOUNT_TEMPORARILY_LOCKED");
        assertThat(temp.httpStatus()).isEqualTo(423);
        assertThat(temp.unlocksAt()).isEqualTo(LATER);

        for (AccountStatus status : new AccountStatus[] {AccountStatus.LOCKED, AccountStatus.INACTIVE}) {
            Decision d = LoginRules.decide(status, null, true, NOW);
            assertThat(d.errorCode()).isEqualTo("ACCOUNT_CONTACT_FACILITY");
            assertThat(d.httpStatus()).isEqualTo(423);
        }
    }

    @Test
    void FR_008_expiredTemporaryLock_actsAsActive() {
        assertThat(LoginRules.decide(AccountStatus.TEMPORARILY_LOCKED, NOW, true, NOW)
                        .allowed())
                .isTrue();
        assertThat(LoginRules.decide(AccountStatus.ACTIVE, null, true, NOW).allowed())
                .isTrue();
        assertThat(LoginRules.decide(AccountStatus.TEMPORARILY_LOCKED, NOW, false, NOW)
                        .countsFailure())
                .isTrue();
        assertThat(LoginRules.decide(AccountStatus.TEMPORARILY_LOCKED, LATER, false, NOW)
                        .countsFailure())
                .isFalse();
    }
}
