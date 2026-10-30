package com.eldercare.backend.administration.auditlog.internal.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.AuditLogRecord;
import com.eldercare.backend.administration.auditlog.internal.persistence.AuditLogStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
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

    private final AuditLogStore store;
    private final BusinessTime time;
    private final JsonMapper json;
    private final ObjectProvider<CurrentUser> currentUser;

    AuditLogService(AuditLogStore store, BusinessTime time, JsonMapper json, ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
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
        return store.recorded(action.code(), entityType, entityId, actorAccountId, RECORDED_LIMIT);
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
        return store.recordedBetween(entityType, from, to, afterId, limit);
    }

    private void insert(AuditLogEntry entry, String actorType, Long actorId, String role) {
        store.insert(
                time.now(),
                entry.deviceTime(),
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

    private String toJson(Object value) {
        return value == null ? null : json.writeValueAsString(value);
    }
}
