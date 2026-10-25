package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.m15.account.api.AccountApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Họ tên người ghi theo hồ sơ tài khoản (002); không có thì dùng mã tài khoản. */
@Component
public class M02ActorNames {

    private final ObjectProvider<AccountApi> accounts;

    M02ActorNames(ObjectProvider<AccountApi> accounts) {
        this.accounts = accounts;
    }

    public String of(long accountId) {
        AccountApi api = accounts.getIfAvailable();
        return api == null ? "#" + accountId : api.fullName(accountId).orElse("#" + accountId);
    }
}
