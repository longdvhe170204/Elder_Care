package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Xác thực bằng phiên của 002 trong {@code SecurityContext}; tên là id tài khoản (khóa người gửi của
 * {@code IdempotencyFilter}). Quyền không nằm ở đây: tính ở {@code PermissionApi} mỗi yêu cầu (research A-05, A-10).
 */
public class SessionAuthentication extends AbstractAuthenticationToken {

    private final ActiveSession session;

    public SessionAuthentication(ActiveSession session) {
        super(List.of());
        this.session = session;
        setAuthenticated(true);
    }

    @Override
    public ActiveSession getPrincipal() {
        return session;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getName() {
        return Long.toString(session.accountId());
    }
}
