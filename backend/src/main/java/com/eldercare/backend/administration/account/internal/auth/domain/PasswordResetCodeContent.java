package com.eldercare.backend.administration.account.internal.auth.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Mã đặt lại mật khẩu ở {@code simple_record} loại {@code M15.PASSWORD_RESET_CODE} (002 T105; FR-052a; gộp bảng
 * 2026-11-13, research R-15 của 000): cha là tài khoản, {@code created_at} là lúc xin mã; trạng thái {@code ACTIVE},
 * {@code USED}, {@code REVOKED} (có mã mới, hoặc sai đủ CFG-M15-13 lần). Chỉ lưu SHA-256 (hex) của id tài khoản ghép mã.
 */
public record PasswordResetCodeContent(String codeHash, Instant expiresAt, int failedAttempts) {

    public static final SharedKind<PasswordResetCodeContent> KIND =
            SharedKind.of("M15.PASSWORD_RESET_CODE", PasswordResetCodeContent.class);

    public static final String ENTITY = "M15.ACCOUNT";
    public static final String ACTIVE = "ACTIVE";
    public static final String USED = "USED";
    public static final String REVOKED = "REVOKED";

    public PasswordResetCodeContent failedAgain() {
        return new PasswordResetCodeContent(codeHash, expiresAt, failedAttempts + 1);
    }
}
