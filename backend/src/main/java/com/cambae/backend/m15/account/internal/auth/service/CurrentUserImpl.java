package com.cambae.backend.m15.account.internal.auth.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.auth.domain.SchedulerUser;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Fallback;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@code CurrentUser} của ứng dụng (002 T012; FR-044): khi job chạy là Bộ lập lịch ({@link SchedulerUserAspect}), trong
 * yêu cầu là người dùng của phiên ({@link SessionAuthentication}); không có cả hai thì lỗi. Mỗi lần gọi đọc lại ngữ
 * cảnh hiện tại nên tiêm một lần dùng được cho mọi yêu cầu. {@code @Fallback}: test tự khai bean {@code CurrentUser} giả
 * thì bean giả được dùng.
 */
@Component
@Fallback
class CurrentUserImpl implements CurrentUser {

    private static CurrentUser current() {
        if (SchedulerUserAspect.isRunning()) {
            return SchedulerUser.INSTANCE;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof SessionAuthentication session) {
            return session.getPrincipal();
        }
        throw new IllegalStateException("Không có người dùng hiện tại: không có phiên và không phải job đang chạy");
    }

    @Override
    public long accountId() {
        return current().accountId();
    }

    @Override
    public AccountType type() {
        return current().type();
    }

    @Override
    public Optional<Long> staffId() {
        return current().staffId();
    }

    @Override
    public Optional<Long> familyMemberId() {
        return current().familyMemberId();
    }

    @Override
    public Optional<Long> residentId() {
        return current().residentId();
    }

    @Override
    public Set<Role> roles() {
        return current().roles();
    }

    @Override
    public boolean isScheduler() {
        return current().isScheduler();
    }

    @Override
    public String deviceId() {
        return current().deviceId();
    }

    @Override
    public AccessChannel channel() {
        return current().channel();
    }
}
