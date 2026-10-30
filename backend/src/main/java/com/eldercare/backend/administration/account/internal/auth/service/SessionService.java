package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.domain.SessionRow;
import com.eldercare.backend.administration.account.internal.auth.persistence.SessionStore;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Tra phiên theo token ở mỗi yêu cầu (002 research A-02; FR-013, FR-013a, BR-M15-21): tìm theo khóa chính
 * {@code token_hash}, không đệm giữa các yêu cầu. Không thao tác quá CFG-M15-10 của kênh thì kết thúc phiên
 * {@code TIMED_OUT} (không đổi trạng thái tài khoản); còn hạn thì đếm lại từ đầu và ghi lần kết nối của thiết bị.
 */
@Service
public class SessionService {

    private final SessionStore sessions;
    private final AccountStore accounts;
    private final BusinessTime time;
    private final ParameterApi parameters;

    SessionService(SessionStore sessions, AccountStore accounts, BusinessTime time, ParameterApi parameters) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.time = time;
        this.parameters = parameters;
    }

    /** Phiên của yêu cầu kèm cờ buộc đổi mật khẩu của tài khoản (FR-006, research A-03), đọc cùng lần tra phiên. */
    public record Resolved(ActiveSession session, boolean mustChangePassword) {}

    /** Phiên đang mở của token; rỗng khi token không có, phiên đã kết thúc hoặc vừa hết thời gian không thao tác. */
    public Optional<ActiveSession> resolve(String token) {
        return resolveRequest(token).map(Resolved::session);
    }

    /**
     * Phiên của token cho {@code POST /sync} (FR-041a, BR-M15-20; plan 002 ghi chú Phase 6): nhận cả phiên đã kết thúc
     * (trừ đăng xuất) vì bản ghi được ghi khi phiên còn mở, phản hồi báo phiên đã kết thúc. Phiên còn mở thì đếm lại giờ
     * như {@link #resolveRequest}; không ghi lần kết nối của thiết bị để lệnh đồng bộ đối chiếu lần kết nối trước khi
     * ngoại tuyến rồi mới cập nhật (T056).
     */
    public Optional<ActiveSession> resolveForSync(String token) {
        byte[] hash = SessionTokens.hash(token);
        Optional<SessionRow> row = sessions.forSync(hash);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        SessionRow found = row.get();
        Instant now = time.now();
        if (found.open()) {
            if (idle(found, now)) {
                sessions.timeOut(hash, now);
            } else {
                sessions.touch(hash, now);
            }
        }
        return Optional.of(session(found, now));
    }

    /** Như {@link #resolve} kèm cờ buộc đổi mật khẩu cho bộ lọc phiên. */
    public Optional<Resolved> resolveRequest(String token) {
        byte[] hash = SessionTokens.hash(token);
        Optional<SessionRow> row = sessions.open(hash);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        SessionRow open = row.get();
        Instant now = time.now();
        if (idle(open, now)) {
            sessions.timeOut(hash, now);
            return Optional.empty();
        }
        sessions.touch(hash, now);
        sessions.recordConnection(open.deviceId(), now, open.accountId());
        return Optional.of(new Resolved(session(open, now), open.mustChangePassword()));
    }

    private boolean idle(SessionRow row, Instant now) {
        Optional<Duration> limit = idleLimit(row.channel());
        return limit.isPresent() && Duration.between(row.lastActiveAt(), now).compareTo(limit.get()) > 0;
    }

    /** Vai trò đang hiệu lực tại {@code now} (research A-05: {@code effective_from <= t < effective_to}). */
    private ActiveSession session(SessionRow row, Instant now) {
        return new ActiveSession(
                row.accountId(),
                row.type(),
                row.staffId(),
                row.familyMemberId(),
                Set.copyOf(accounts.rolesAt(row.accountId(), now)),
                row.deviceId(),
                row.channel(),
                row.residentId());
    }

    /** CFG-M15-10 theo kênh: {@code {"value", "unit"}} hoặc {@code {"noLimit": true}} (kênh người cao tuổi, BR-M15-24). */
    private Optional<Duration> idleLimit(AccessChannel channel) {
        JsonNode v = parameters.read(ParameterCode.CFG_M15_10).raw().get(channel.name());
        if (v == null) {
            throw new IllegalStateException("CFG-M15-10 thiếu kênh " + channel);
        }
        if (v.has("noLimit") && v.get("noLimit").asBoolean()) {
            return Optional.empty();
        }
        return Optional.of(Duration.of(
                v.get("value").longValue(), ChronoUnit.valueOf(v.get("unit").asString())));
    }
}
