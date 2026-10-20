package com.cambae.backend.m15.account.internal.domain;

import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import java.time.Instant;

/**
 * Tài khoản (data-model mục 2): trạng thái, liên kết hồ sơ, phiên bản cho {@code If-Match} và tập quyền. Dùng chung cho
 * các chức năng của {@code m15.account}; {@code AccountStore} đọc ghi bảng {@code account}.
 */
public record Account(
        long id,
        String username,
        AccountType type,
        Long staffId,
        Long familyMemberId,
        AccountStatus status,
        String lockCause,
        Instant lockedUntil,
        Instant lastLoginAt,
        int version,
        int permissionVersion) {}
