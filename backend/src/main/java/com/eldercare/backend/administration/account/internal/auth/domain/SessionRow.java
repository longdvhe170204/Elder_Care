package com.eldercare.backend.administration.account.internal.auth.domain;

import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import java.time.Instant;
import java.util.Optional;

/** Một phiên tra theo token kèm phần tài khoản cần cho yêu cầu (002 research A-02); {@code open} khi chưa kết thúc. */
public record SessionRow(
        long accountId,
        String deviceId,
        AccessChannel channel,
        Instant lastActiveAt,
        AccountType type,
        Optional<Long> staffId,
        Optional<Long> familyMemberId,
        boolean mustChangePassword,
        Optional<Long> residentId,
        boolean open) {}
