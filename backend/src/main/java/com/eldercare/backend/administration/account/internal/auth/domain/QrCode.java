package com.eldercare.backend.administration.account.internal.auth.domain;

import java.time.Instant;

/** Một mã QR đăng nhập; dùng được khi chưa dùng, chưa vô hiệu và {@code expiresAt} sau thời điểm kiểm (FR-069). */
public record QrCode(long id, long accountId, Instant expiresAt, boolean used, boolean revoked) {

    public boolean usableAt(Instant now) {
        return !used && !revoked && expiresAt.isAfter(now);
    }
}
