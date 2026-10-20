package com.cambae.backend.m15.account.auth.internal;

import com.cambae.backend.m15.account.api.AccountStatus;
import java.time.Instant;

/**
 * Quyết định một lần đăng nhập (002 research A-03; FR-007, FR-008, FR-014; NFR-08): sai tên hoặc sai mật khẩu, kể cả
 * khi tài khoản không Hoạt động, đều là {@code LOGIN_FAILED}; chỉ mật khẩu đúng mới cho biết khóa tạm (kèm thời điểm
 * mở) hoặc cần liên hệ viện. Khóa tạm đã hết hạn coi như Hoạt động. Lần sai chỉ đếm khi tài khoản đang Hoạt động.
 */
final class LoginRules {

    static final String LOGIN_FAILED = "LOGIN_FAILED";

    /** Kết quả; {@code allowed} thì các trường lỗi rỗng. */
    record Decision(boolean allowed, String errorCode, int httpStatus, Instant unlocksAt, boolean countsFailure) {

        static final Decision ALLOW = new Decision(true, null, 200, null, false);

        static Decision refuse(String errorCode, int httpStatus, Instant unlocksAt, boolean countsFailure) {
            return new Decision(false, errorCode, httpStatus, unlocksAt, countsFailure);
        }
    }

    private LoginRules() {}

    /** {@code status} {@code null} khi không có tài khoản mang tên đăng nhập này. */
    static Decision decide(AccountStatus status, Instant lockedUntil, boolean passwordMatches, Instant now) {
        AccountStatus effective = effective(status, lockedUntil, now);
        if (!passwordMatches) {
            return Decision.refuse(LOGIN_FAILED, 401, null, effective == AccountStatus.ACTIVE);
        }
        if (effective == null) {
            return Decision.refuse(LOGIN_FAILED, 401, null, false);
        }
        return switch (effective) {
            case ACTIVE -> Decision.ALLOW;
            case TEMPORARILY_LOCKED -> Decision.refuse("ACCOUNT_TEMPORARILY_LOCKED", 423, lockedUntil, false);
            case INACTIVE, LOCKED -> Decision.refuse("ACCOUNT_CONTACT_FACILITY", 423, null, false);
        };
    }

    /** Khóa tạm hết hạn tính ở yêu cầu kế tiếp: coi như Hoạt động (research A-03). */
    static AccountStatus effective(AccountStatus status, Instant lockedUntil, Instant now) {
        if (status == AccountStatus.TEMPORARILY_LOCKED && lockedUntil != null && !lockedUntil.isAfter(now)) {
            return AccountStatus.ACTIVE;
        }
        return status;
    }
}
