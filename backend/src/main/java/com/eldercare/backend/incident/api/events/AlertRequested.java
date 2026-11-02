package com.eldercare.backend.incident.api.events;

import java.time.Instant;

/** Sự kiện/yêu cầu công khai theo hợp đồng 007 mục 1.12–1.13; không trực tiếp gửi thông báo. */
public record AlertRequested(
        long residentId,
        String type,
        String level,
        String sourceType,
        long sourceId,
        java.util.Optional<String> objectKey,
        Instant occurredAt,
        String eventKey) {}
