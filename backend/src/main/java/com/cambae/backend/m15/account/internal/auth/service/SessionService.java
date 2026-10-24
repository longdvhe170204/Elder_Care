package com.cambae.backend.m15.account.internal.auth.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Tra phiên theo token ở mỗi yêu cầu (002 research A-02; FR-013, FR-013a, BR-M15-21): tìm theo khóa chính
 * {@code token_hash}, không đệm giữa các yêu cầu. Không thao tác quá CFG-M15-10 của kênh thì kết thúc phiên
 * {@code TIMED_OUT} (không đổi trạng thái tài khoản); còn hạn thì đếm lại từ đầu và ghi lần kết nối của thiết bị.
 */
@Service
public class SessionService {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final ParameterApi parameters;

    SessionService(JdbcTemplate jdbc, BusinessTime time, ParameterApi parameters) {
        this.jdbc = jdbc;
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
        Optional<SyncRow> row = jdbc
                .query(
                        "SELECT s.account_id, s.device_id, s.channel, s.last_active_at, a.type, a.staff_id,"
                                + " a.family_member_id, a.must_change_password, s.ended_at IS NULL, a.resident_id FROM login_session s"
                                + " JOIN account a ON a.id = s.account_id"
                                + " WHERE s.token_hash = ? AND (s.end_reason IS NULL OR s.end_reason <> 'LOGOUT')",
                        (rs, i) -> new SyncRow(
                                new OpenRow(
                                        rs.getLong(1),
                                        rs.getString(2),
                                        AccessChannel.valueOf(rs.getString(3)),
                                        rs.getObject(4, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                                        AccountType.valueOf(rs.getString(5)),
                                        Optional.ofNullable(rs.getObject(6, Long.class)),
                                        Optional.ofNullable(rs.getObject(7, Long.class)),
                                        rs.getBoolean(8),
                                        Optional.ofNullable(rs.getObject(10, Long.class))),
                                rs.getBoolean(9)),
                        hash)
                .stream()
                .findFirst();
        if (row.isEmpty()) {
            return Optional.empty();
        }
        OpenRow found = row.get().session();
        Instant now = time.now();
        if (row.get().open()) {
            Optional<Duration> limit = idleLimit(found.channel());
            String column = limit.isPresent()
                            && Duration.between(found.lastActiveAt(), now).compareTo(limit.get()) > 0
                    ? "ended_at = ?, end_reason = 'TIMED_OUT'"
                    : "last_active_at = ?";
            jdbc.update(
                    "UPDATE login_session SET " + column + " WHERE token_hash = ? AND ended_at IS NULL",
                    utc(now),
                    hash);
        }
        return Optional.of(new ActiveSession(
                found.accountId(),
                found.type(),
                found.staffId(),
                found.familyMemberId(),
                roles(found.accountId(), now),
                found.deviceId(),
                found.channel(),
                found.residentId()));
    }

    /** Như {@link #resolve} kèm cờ buộc đổi mật khẩu cho bộ lọc phiên. */
    public Optional<Resolved> resolveRequest(String token) {
        byte[] hash = SessionTokens.hash(token);
        Optional<OpenRow> row = jdbc
                .query(
                        "SELECT s.account_id, s.device_id, s.channel, s.last_active_at, a.type, a.staff_id,"
                                + " a.family_member_id, a.must_change_password, a.resident_id FROM login_session s"
                                + " JOIN account a ON a.id = s.account_id"
                                + " WHERE s.token_hash = ? AND s.ended_at IS NULL",
                        (rs, i) -> new OpenRow(
                                rs.getLong(1),
                                rs.getString(2),
                                AccessChannel.valueOf(rs.getString(3)),
                                rs.getObject(4, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                                AccountType.valueOf(rs.getString(5)),
                                Optional.ofNullable(rs.getObject(6, Long.class)),
                                Optional.ofNullable(rs.getObject(7, Long.class)),
                                rs.getBoolean(8),
                                Optional.ofNullable(rs.getObject(9, Long.class))),
                        hash)
                .stream()
                .findFirst();
        if (row.isEmpty()) {
            return Optional.empty();
        }
        OpenRow open = row.get();
        Instant now = time.now();
        Optional<Duration> limit = idleLimit(open.channel());
        if (limit.isPresent() && Duration.between(open.lastActiveAt(), now).compareTo(limit.get()) > 0) {
            jdbc.update(
                    "UPDATE login_session SET ended_at = ?, end_reason = 'TIMED_OUT'"
                            + " WHERE token_hash = ? AND ended_at IS NULL",
                    utc(now),
                    hash);
            return Optional.empty();
        }
        jdbc.update(
                "UPDATE login_session SET last_active_at = ? WHERE token_hash = ? AND ended_at IS NULL",
                utc(now),
                hash);
        jdbc.update(
                "INSERT INTO device (device_id, last_connected_at, last_account_id) VALUES (?, ?, ?) AS new"
                        + " ON DUPLICATE KEY UPDATE last_connected_at = new.last_connected_at,"
                        + " last_account_id = new.last_account_id",
                open.deviceId(),
                utc(now),
                open.accountId());
        return Optional.of(new Resolved(
                new ActiveSession(
                        open.accountId(),
                        open.type(),
                        open.staffId(),
                        open.familyMemberId(),
                        roles(open.accountId(), now),
                        open.deviceId(),
                        open.channel(),
                        open.residentId()),
                open.mustChangePassword()));
    }

    /** Vai trò đang hiệu lực tại {@code at} (research A-05: {@code effective_from <= t < effective_to}). */
    private Set<Role> roles(long accountId, Instant at) {
        Map<String, Role> byCode = new HashMap<>();
        for (Role role : Role.values()) {
            byCode.put(role.code(), role);
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        jdbc.query(
                "SELECT role FROM role_assignment WHERE account_id = ? AND effective_from <= ?"
                        + " AND (effective_to IS NULL OR effective_to > ?)",
                rs -> {
                    roles.add(byCode.get(rs.getString(1)));
                },
                accountId,
                utc(at),
                utc(at));
        return Set.copyOf(roles);
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

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record OpenRow(
            long accountId,
            String deviceId,
            AccessChannel channel,
            Instant lastActiveAt,
            AccountType type,
            Optional<Long> staffId,
            Optional<Long> familyMemberId,
            boolean mustChangePassword,
            Optional<Long> residentId) {}

    private record SyncRow(OpenRow session, boolean open) {}
}
