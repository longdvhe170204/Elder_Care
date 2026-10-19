package com.cambae.backend.m15.auditlog.internal;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.Role;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ghi nhật ký bằng JDBC trong transaction của lệnh (research R-06). Người thực hiện lấy từ {@link CurrentUser} (002
 * cung cấp); thời điểm lấy từ đồng hồ máy chủ (FR-046).
 */
@Service
class AuditLogService implements AuditLogApi {

    private static final String INSERT = "INSERT INTO audit_log (occurred_at, device_time, actor_type, actor_id, role,"
            + " action, entity_type, entity_id, resident_id, data_before, data_after, reason, basis, details,"
            + " exempt_from_purge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)";

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

    private void insert(AuditLogEntry entry) {
        CurrentUser user = currentUser.getObject();
        if (user.isScheduler() && entry.basis() == null) {
            throw new IllegalStateException("Thay đổi do Bộ lập lịch phải có căn cứ (FR-045)");
        }
        jdbc.update(
                INSERT,
                utc(time.now()),
                entry.deviceTime() == null ? null : utc(entry.deviceTime()),
                user.isScheduler() ? "SCHEDULER" : "USER",
                user.isScheduler() ? null : user.accountId(),
                roleCode(user),
                entry.action().code(),
                entry.entityType(),
                entry.entityId(),
                entry.residentId(),
                toJson(entry.before()),
                toJson(entry.after()),
                entry.reason(),
                toJson(entry.basis()),
                toJson(entry.details()));
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
