package com.eldercare.backend.administration.auditlog.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Bản ghi nhật ký đã lưu, cho module ghi đọc lại bản ghi của chính hành động mình ({@link AuditLogApi#recorded}; 016
 * FR-062 lần xuất). {@code actorAccountId} rỗng khi người thực hiện không phải tài khoản (Bộ lập lịch, khởi tạo).
 */
public record AuditLogRecord(
        long id,
        Instant occurredAt,
        Long actorAccountId,
        String role,
        String entityId,
        String reason,
        Map<String, Object> details) {}
