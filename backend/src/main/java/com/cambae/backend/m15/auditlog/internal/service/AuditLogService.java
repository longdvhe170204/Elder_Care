package com.cambae.backend.m15.auditlog.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.AuditLogRecord;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ghi nhật ký bằng JDBC trong transaction của lệnh (research R-06). Người thực hiện lấy từ {@link CurrentUser} (002
 * cung cấp); thời điểm lấy từ đồng hồ máy chủ (FR-046).
 */
@Service
class AuditLogService implements AuditLogApi {

    private static final String INSERT = "INSERT INTO audit_log (occurred_at, device_time, actor_type, actor_id, role,"
            + " action, entity_type, entity_id, resident_id, data_before, data_after, reason, basis, details,"
            + " exempt_from_purge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final JsonMapper json;
    private final ObjectProvider<CurrentUser> currentUser;

    AuditLogService(JdbcTemplate jdbc, BusinessTime time, JsonMapper json, ObjectProvider<CurrentUser> currentUser) {
        this.jdbc = jdbc;
        this.time = time;
        this.json = json;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(AuditLogEntry entry) {
        insert(entry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeOutsideTransaction(AuditLogEntry entry) {
        insert(entry);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeAsBootstrap(AuditLogEntry entry) {
        if (entry.basis() == null) {
            throw new IllegalStateException("Thay đổi do khởi tạo hệ thống phải có căn cứ (FR-045)");
        }
        insert(entry, "BOOTSTRAP", null, null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeAnonymous(AuditLogEntry entry) {
        insert(entry, "ANONYMOUS", null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogRecord> recorded(AuditAction action, String entityType, String entityId, Long actorAccountId) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, occurred_at, actor_type, actor_id, role, entity_id, reason, details FROM audit_log"
                        + " WHERE action = ? AND entity_type = ?");
        List<Object> args = new ArrayList<>(List.of(action.code(), entityType));
        if (entityId != null) {
            sql.append(" AND entity_id = ?");
            args.add(entityId);
        }
        if (actorAccountId != null) {
            sql.append(" AND actor_type = 'USER' AND actor_id = ?");
            args.add(actorAccountId);
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ").append(RECORDED_LIMIT);
        return jdbc.query(
                sql.toString(),
                (rs, i) -> new AuditLogRecord(
                        rs.getLong("id"),
                        rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        "USER".equals(rs.getString("actor_type")) ? rs.getLong("actor_id") : null,
                        rs.getString("role"),
                        rs.getString("entity_id"),
                        rs.getString("reason"),
                        rs.getString("details") == null ? null : json.readValue(rs.getString("details"), MAP)),
                args.toArray());
    }

    private void insert(AuditLogEntry entry) {
        CurrentUser user = currentUser.getObject();
        if (user.isScheduler() && entry.basis() == null) {
            throw new IllegalStateException("Thay đổi do Bộ lập lịch phải có căn cứ (FR-045)");
        }
        insert(
                entry,
                user.isScheduler() ? "SCHEDULER" : "USER",
                user.isScheduler() ? null : user.accountId(),
                roleCode(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogRecord> recordedBetween(String entityType, Instant from, Instant to, long afterId, int limit) {
        if (entityType == null
                || entityType.isBlank()
                || from == null
                || to == null
                || !from.isBefore(to)
                || afterId < 0
                || limit < 1
                || limit > RECORDED_LIMIT) {
            throw new IllegalArgumentException("Khoảng nhật ký hoặc trang đọc không hợp lệ");
        }
        return jdbc.query(
                "SELECT id, occurred_at, actor_type, actor_id, role, entity_id, reason, details FROM audit_log"
                        + " WHERE entity_type=? AND occurred_at>=? AND occurred_at<? AND id>? ORDER BY id LIMIT ?",
                (rs, i) -> new AuditLogRecord(
                        rs.getLong("id"),
                        rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        "USER".equals(rs.getString("actor_type")) ? rs.getLong("actor_id") : null,
                        rs.getString("role"),
                        rs.getString("entity_id"),
                        rs.getString("reason"),
                        rs.getString("details") == null ? null : json.readValue(rs.getString("details"), MAP)),
                entityType,
                utc(from),
                utc(to),
                afterId,
                limit);
    }

    private void insert(AuditLogEntry entry, String actorType, Long actorId, String role) {
        jdbc.update(
                INSERT,
                utc(time.now()),
                entry.deviceTime() == null ? null : utc(entry.deviceTime()),
                actorType,
                actorId,
                role,
                entry.action().code(),
                entry.entityType(),
                entry.entityId(),
                entry.residentId(),
                toJson(entry.before()),
                toJson(entry.after()),
                entry.reason(),
                toJson(entry.basis()),
                toJson(entry.details()),
                // Chỉ bản tóm tắt đợt loại bỏ không bị loại bỏ (FR-051); module khác không tự đánh dấu được
                entry.action().equals(AuditAction.EXPIRED_DATA_PURGED));
    }

    /** Vai trò tại lúc thực hiện; tài khoản nhiều vai trò lấy vai trò đầu theo thứ tự enum (002 có thể đổi). */
    private static String roleCode(CurrentUser user) {
        if (user.isScheduler()) {
            return Role.SCHEDULER.code();
        }
        return user.roles().stream()
                .min(Comparator.naturalOrder())
                .map(Role::code)
                .orElse(null);
    }

    /** Cột DATETIME(6) lưu UTC (R-04); LocalDateTime được driver gửi nguyên, không đổi múi giờ. */
    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String toJson(Object value) {
        return value == null ? null : json.writeValueAsString(value);
    }
}
