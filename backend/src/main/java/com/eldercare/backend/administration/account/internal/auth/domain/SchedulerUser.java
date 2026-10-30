package com.eldercare.backend.administration.account.internal.auth.domain;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.common.Role;
import java.util.Optional;
import java.util.Set;

/**
 * Người dùng hiện tại khi job chạy: Bộ lập lịch hệ thống AC-11 (002 FR-044; 000 FR-045). Không có tài khoản: {@link
 * #accountId()} là 0, nhật ký kiểm {@link #isScheduler()} trước nên không ghi id này. Không có phiên nên không có thiết
 * bị, kênh.
 */
public final class SchedulerUser implements CurrentUser {

    public static final SchedulerUser INSTANCE = new SchedulerUser();

    private SchedulerUser() {}

    @Override
    public long accountId() {
        return 0;
    }

    @Override
    public AccountType type() {
        return AccountType.STAFF;
    }

    @Override
    public Optional<Long> staffId() {
        return Optional.empty();
    }

    @Override
    public Optional<Long> familyMemberId() {
        return Optional.empty();
    }

    @Override
    public Optional<Long> residentId() {
        return Optional.empty();
    }

    @Override
    public Set<Role> roles() {
        return Set.of(Role.SCHEDULER);
    }

    @Override
    public boolean isScheduler() {
        return true;
    }

    @Override
    public String deviceId() {
        throw new IllegalStateException("Bộ lập lịch không có phiên, không có thiết bị");
    }

    @Override
    public AccessChannel channel() {
        throw new IllegalStateException("Bộ lập lịch không có phiên, không có kênh");
    }
}
