package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Cảnh báo hoặc sự cố đang mở theo 007 mục 1.5. */
public record AlertHandoverView(
        long id,
        String group,
        long residentId,
        Long roomId,
        String description,
        String status,
        String level,
        Instant createdAt) {}
