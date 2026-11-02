package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import org.springframework.stereotype.Service;

/** Họ tên người thực hiện cho phản hồi api-006 (như {@code Dose.closedBy}); chưa có hồ sơ liên kết thì {@code null}. */
@Service("m07ActorNames")
public class ActorNames {

    private final AccountApi accounts;

    public ActorNames(AccountApi accounts) {
        this.accounts = accounts;
    }

    public String of(Long accountId) {
        return accountId == null ? null : accounts.fullName(accountId).orElse(null);
    }
}
