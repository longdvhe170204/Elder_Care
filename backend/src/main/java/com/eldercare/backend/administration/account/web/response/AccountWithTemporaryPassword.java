package com.eldercare.backend.administration.account.web.response;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

// Hợp đồng ghi allOf(Account, {temporaryPassword}); JSON vẫn là một object phẳng
@Schema(
        name = "AccountWithTemporaryPassword",
        allOf = {AccountBody.class, TemporaryPassword.class})
public record AccountWithTemporaryPassword(
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
        List<String> allowedCommands,
        String temporaryPassword) {

    public static AccountWithTemporaryPassword of(AccountService.Created created, String fullName) {
        AccountBody a = AccountBody.of(created.details(), fullName);
        return new AccountWithTemporaryPassword(
                a.id(),
                a.username(),
                a.fullName(),
                a.type(),
                a.staffId(),
                a.familyMemberId(),
                a.residentId(),
                a.boundDevice(),
                a.status(),
                a.lockCause(),
                a.lockedUntil(),
                a.lastLoginAt(),
                a.roles(),
                a.allowedCommands(),
                created.temporaryPassword());
    }
}
