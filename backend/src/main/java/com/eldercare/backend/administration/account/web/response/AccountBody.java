package com.eldercare.backend.administration.account.web.response;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService.AccountDetails;
import com.eldercare.backend.administration.account.internal.domain.Account;
import com.eldercare.backend.common.Role;
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
        Account a = d.account();
        return new AccountBody(
                a.id(),
                a.username(),
                fullName,
                a.type(),
                a.staffId(),
                a.familyMemberId(),
                a.residentId(),
                d.boundDevice() == null
                        ? null
                        : new BoundDevice(
                                d.boundDevice().boundAt(), d.boundDevice().deviceId()),
                a.status(),
                a.lockCause(),
                a.lockedUntil(),
                a.lastLoginAt(),
                d.roles().stream().map(Role::code).sorted().toList(),
                d.allowedCommands());
    }
}
