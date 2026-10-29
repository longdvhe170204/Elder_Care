package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.internal.channel.domain.ChannelConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Kênh thông báo (schema {@code NotificationChannel}; FR-016). */
@Schema(name = "NotificationChannel")
public record NotificationChannelBody(String code, String name, boolean enabled, List<NotificationLevel> levels) {

    public static NotificationChannelBody of(ChannelConfig c) {
        return new NotificationChannelBody(
                c.code(), c.name(), c.enabled(), c.levels().stream().sorted().toList());
    }
}
