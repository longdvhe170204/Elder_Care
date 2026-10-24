package com.cambae.backend.m15.account.internal.auth.domain;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import java.util.Optional;
import java.util.Set;

/**
 * Phiên đang mở của yêu cầu hiện tại, cũng là người dùng hiện tại của yêu cầu (002 research A-02; T012): loại, liên kết
 * hồ sơ, vai trò đang hiệu lực đọc cùng lần tra phiên.
 */
public record ActiveSession(
        long accountId,
        AccountType type,
        Optional<Long> staffId,
        Optional<Long> familyMemberId,
        Set<Role> roles,
        String deviceId,
        AccessChannel channel,
        Optional<Long> residentId)
        implements CurrentUser {

    /** Phiên của tài khoản nhân viên, người thân (không liên kết người cao tuổi). */
    public ActiveSession(
            long accountId,
            AccountType type,
            Optional<Long> staffId,
            Optional<Long> familyMemberId,
            Set<Role> roles,
            String deviceId,
            AccessChannel channel) {
        this(accountId, type, staffId, familyMemberId, roles, deviceId, channel, Optional.empty());
    }

    @Override
    public boolean isScheduler() {
        return false;
    }
}
