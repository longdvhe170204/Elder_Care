package com.eldercare.backend.administration.account.web.security;

import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.service.SessionAuthentication;
import com.eldercare.backend.administration.account.internal.auth.service.SessionService;
import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Xác thực mỗi yêu cầu bằng token phiên (002 research A-02, A-15; FR-013, FR-013a): app gửi {@code Authorization:
 * Bearer}, web gửi cookie {@code session} và mọi lệnh ghi phải kèm {@code X-Requested-From: web} (chặn CSRF). Có token
 * mà không còn phiên thì 401 {@code SESSION_EXPIRED}; không có token thì để Spring Security trả 401. Ba đường không
 * cần phiên và không tra phiên: đăng nhập, đăng nhập bằng mã QR, {@code /sos/**} (007 xác thực bằng token thiết bị).
 */
class SessionTokenFilter extends OncePerRequestFilter {

    static final RequestMatcher PUBLIC_PATHS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/login"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/qr-login"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/sos/**"));
    /**
     * Phiên buộc đổi mật khẩu chỉ gọi được đổi mật khẩu, đăng xuất (FR-006, research A-03) và đọc {@code GET /me} để
     * client biết phải đổi mật khẩu ({@code UserInfo.mustChangePassword}; plan 002 ghi chú Phase 5).
     */
    static final RequestMatcher PASSWORD_CHANGE_PATHS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/change-password"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/logout"),
            PathPatternRequestMatcher.withDefaults().matcher(org.springframework.http.HttpMethod.GET, "/api/v1/me"));

    /**
     * Đồng bộ ngoại tuyến nhận cả phiên đã kết thúc (trừ đăng xuất) và không bắt đổi mật khẩu: bản ghi được ghi khi phiên
     * còn mở, phản hồi báo phiên đã kết thúc (FR-041a, BR-M15-20; {@code SessionService.resolveForSync}).
     */
    static final RequestMatcher SYNC =
            PathPatternRequestMatcher.withDefaults().matcher(org.springframework.http.HttpMethod.POST, "/api/v1/sync");

    static final String REQUESTED_FROM = "X-Requested-From";
    private static final String BEARER = "Bearer ";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final SessionService sessions;
    private final JsonMapper json;

    SessionTokenFilter(SessionService sessions, JsonMapper json) {
        this.sessions = sessions;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<String> cookie = cookie(request);
        String token = header != null && header.startsWith(BEARER)
                ? header.substring(BEARER.length()).strip()
                : cookie.orElse(null);
        if (token == null || token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        boolean viaCookie = token.equals(cookie.orElse(null));
        if (viaCookie
                && !SAFE_METHODS.contains(request.getMethod())
                && !"web".equals(request.getHeader(REQUESTED_FROM))) {
            writeProblem(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Lệnh ghi từ web phải có " + REQUESTED_FROM);
            return;
        }
        if (SYNC.matches(request)) {
            Optional<ActiveSession> syncSession = sessions.resolveForSync(token);
            if (syncSession.isEmpty()) {
                writeProblem(
                        response, HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Phiên đã kết thúc, cần đăng nhập lại");
                return;
            }
            authenticate(syncSession.get());
            chain.doFilter(request, response);
            return;
        }
        Optional<SessionService.Resolved> session = sessions.resolveRequest(token);
        if (session.isEmpty()) {
            writeProblem(response, HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Phiên đã kết thúc, cần đăng nhập lại");
            return;
        }
        if (session.get().mustChangePassword() && !PASSWORD_CHANGE_PATHS.matches(request)) {
            writeProblem(
                    response,
                    HttpStatus.FORBIDDEN,
                    "PASSWORD_CHANGE_REQUIRED",
                    "Cần đổi mật khẩu trước khi dùng chức năng khác");
            return;
        }
        authenticate(session.get().session());
        chain.doFilter(request, response);
    }

    private static void authenticate(ActiveSession session) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionAuthentication(session));
        SecurityContextHolder.setContext(context);
    }

    private static Optional<String> cookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies == null
                ? Optional.empty()
                : Arrays.stream(cookies)
                        .filter(c -> SessionTokens.COOKIE.equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst();
    }

    void writeProblem(HttpServletResponse response, HttpStatus status, String errorCode, String detail)
            throws IOException {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("errorCode", errorCode);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(problem));
    }
}
