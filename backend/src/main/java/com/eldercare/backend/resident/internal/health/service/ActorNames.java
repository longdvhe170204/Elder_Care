package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Họ tên người ghi, người xác nhận theo hồ sơ tài khoản (002); không có thì dùng mã tài khoản. */
@Component
public class ActorNames {

    private final ObjectProvider<AccountApi> accounts;

    ActorNames(ObjectProvider<AccountApi> accounts) {
        this.accounts = accounts;
    }

    public String of(long accountId) {
        AccountApi api = accounts.getIfAvailable();
        return api == null ? "#" + accountId : api.fullName(accountId).orElse("#" + accountId);
    }
}
