package com.cambae.backend.support;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import java.util.Optional;
import java.util.Set;

/** Người dùng hiện tại đổi được giữa các bước của test (bản giả của 002 {@code CurrentUser}). */
public class SwitchableUser implements CurrentUser {

    private volatile CurrentUser delegate = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);

    public void set(CurrentUser user) {
        this.delegate = user;
    }

    @Override
    public long accountId() {
        return delegate.accountId();
    }

    @Override
    public AccountType type() {
        return delegate.type();
    }

    @Override
    public Optional<Long> staffId() {
        return delegate.staffId();
    }

    @Override
    public Optional<Long> familyMemberId() {
        return delegate.familyMemberId();
    }

    @Override
    public Optional<Long> residentId() {
        return delegate.residentId();
    }

    @Override
    public Set<Role> roles() {
        return delegate.roles();
    }

    @Override
    public boolean isScheduler() {
        return delegate.isScheduler();
    }

    @Override
    public String deviceId() {
        return delegate.deviceId();
    }

    @Override
    public AccessChannel channel() {
        return delegate.channel();
    }
}
