package com.eldercare.backend.administration.account.internal.auth.domain;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Quy tắc tự đặt lại mật khẩu (002 T105; FR-052a, BR-M15-25): ai được nhận mã, khi nào được xin thêm (CFG-M15-14), khi
 * nào mã bị hủy (CFG-M15-13). Hàm thuần, không đọc cơ sở dữ liệu.
 */
public final class PasswordResetRules {

    private PasswordResetRules() {}

    /**
     * Nhận mã khi là nhân viên hoặc người thân, trạng thái (đã tính hết hạn khóa tạm) Hoạt động hoặc Khóa tạm, hồ sơ có
     * số điện thoại. Người cao tuổi đăng nhập bằng mã QR (Q-370); Đã khóa, Không hoạt động không mở bằng cách này.
     */
    public static boolean eligible(AccountType type, AccountStatus status, Optional<String> phoneNumber) {
        return type != AccountType.RESIDENT
                && (status == AccountStatus.ACTIVE || status == AccountStatus.TEMPORARILY_LOCKED)
                && phoneNumber.filter(p -> !p.isBlank()).isPresent();
    }

    /** CFG-M15-14: chưa đủ số lần trong ngày và lần gửi trước cách ít nhất {@code minInterval}. */
    public static boolean canRequest(
            Instant now, int sentToday, Optional<Instant> lastSentAt, int maxPerDay, Duration minInterval) {
        return sentToday < maxPerDay
                && lastSentAt.map(last -> !now.isBefore(last.plus(minInterval))).orElse(true);
    }

    /** CFG-M15-13: sai đủ số lần cho phép thì mã bị hủy. */
    public static boolean revokeAfter(int failedAttempts, int maxFailedAttempts) {
        return failedAttempts >= maxFailedAttempts;
    }
}
