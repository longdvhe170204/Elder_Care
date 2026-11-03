package com.eldercare.backend.incident.api.dto;

import java.time.Instant;
import java.util.Optional;

/** Người nhận, kênh và mốc gửi/xem của một thông báo theo bản ghi nguồn (FR-040, FR-048b). */
public record IncidentNotificationView(
        String recipientType, long recipientId, String channel, Optional<Instant> sentAt, Optional<Instant> viewedAt) {}
