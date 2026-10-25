package com.cambae.backend.m13.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.notification.NotificationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/** Thân {@code PUT /notification-channels/{code}}: kênh kèm lý do bắt buộc (FR-016, FR-040). */
public record ChannelChangeBody(
        String code,
        String name,
        boolean enabled,
        Set<NotificationLevel> levels,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", minLength = 1)
        Reason reason) {}
