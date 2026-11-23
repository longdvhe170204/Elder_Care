package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/**
 * Cảnh báo hoặc sự cố đang mở theo 007 mục 1.5. {@code nextStepMissing}: sự cố đang mở chưa có công việc tiếp theo nào chưa
 * đóng (FR-045b); null với cảnh báo hoặc khi chưa đọc được nguồn công việc.
 */
public record AlertHandoverView(
        long id,
        String group,
        long residentId,
        Long roomId,
        String description,
        String status,
        String level,
        Instant createdAt,
        Boolean nextStepMissing) {
    /** Giữ caller cũ: cảnh báo và nguồn không biết việc tiếp theo. */
    public AlertHandoverView(
            long id,
            String group,
            long residentId,
            Long roomId,
            String description,
            String status,
            String level,
            Instant createdAt) {
        this(id, group, residentId, roomId, description, status, level, createdAt, null);
    }
}
