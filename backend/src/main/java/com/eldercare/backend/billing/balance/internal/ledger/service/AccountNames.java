package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import java.util.function.LongFunction;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Họ tên của tài khoản lập, duyệt giao dịch số dư (test BF-16, chủ dự án chọn 2026-11-22): đọc qua
 * {@code AccountApi.fullName}; chưa có hồ sơ hoặc bản cài thì {@code null}.
 */
@Component
public class AccountNames implements LongFunction<String> {

    private final ObjectProvider<AccountApi> accounts;

    AccountNames(ObjectProvider<AccountApi> accounts) {
        this.accounts = accounts;
    }

    @Override
    public String apply(long accountId) {
        AccountApi api = accounts.getIfAvailable();
        return api == null ? null : api.fullName(accountId).orElse(null);
    }
}
