package com.cambae.backend.m15.account.web.response;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.auth.service.AccountService.AccountDetails;
import com.cambae.backend.m15.account.internal.persistence.AccountStore.AccountRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(name = "Account")
public record AccountBody(
        long id,
        String username,
        String fullName,
        AccountType type,
        Long staffId,
        Long familyMemberId,
        Long residentId,
        BoundDevice boundDevice,
        AccountStatus status,
        String lockCause,
        Instant lockedUntil,
        Instant lastLoginAt,
        List<String> roles,
        List<String> allowedCommands) {

    /** {@code fullName} theo hồ sơ liên kết (T032a), không có thì {@code null}. */
    public static AccountBody of(AccountDetails d, String fullName) {
        AccountRow a = d.account();
        return new AccountBody(
                a.id(),
                a.username(),
                fullName,
                a.type(),
                a.staffId(),
                a.familyMemberId(),
                null,
                null,
                a.status(),
                a.lockCause(),
                a.lockedUntil(),
                a.lastLoginAt(),
                d.roles().stream().map(Role::code).sorted().toList(),
                d.allowedCommands());
    }
}
