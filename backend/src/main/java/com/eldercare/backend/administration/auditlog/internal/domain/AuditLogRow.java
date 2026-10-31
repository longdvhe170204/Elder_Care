package com.eldercare.backend.administration.auditlog.internal.domain;

import java.time.LocalDateTime;
import java.util.Map;

/** Một dòng {@code audit_log} đọc cho tra cứu (thời điểm UTC). */
public record AuditLogRow(
        long id,
        LocalDateTime occurredAt,
        LocalDateTime deviceTime,
        String actorType,
        Long actorId,
        String role,
        String action,
        String entityType,
        String entityId,
        Long residentId,
        Map<String, Object> before,
        Map<String, Object> after,
        String reason,
        Map<String, Object> basis,
        Map<String, Object> details) {

    /** Cùng dòng với loại đối tượng hiện hành (loại cũ trước khi gộp bảng, research R-15 mục 5). */
    public AuditLogRow withEntityType(String type) {
        return new AuditLogRow(
                id,
                occurredAt,
                deviceTime,
                actorType,
                actorId,
                role,
                action,
                type,
                entityId,
                residentId,
                before,
                after,
                reason,
                basis,
                details);
    }
}
