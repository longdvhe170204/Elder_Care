package com.eldercare.backend.notification.internal.channel.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.notification.NotificationLevel;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Thuộc tính kênh thông báo ở {@code catalog_item} loại {@code M13.NOTIFICATION_CHANNEL} (009 T043; FR-016, FR-039,
 * FR-040): mã mục là mã kênh; {@code levels} chỉ dùng cho kênh khác (mức được gắn); {@code config} thông tin nhà cung cấp,
 * không chứa bí mật.
 */
public record ChannelAttributes(boolean enabled, List<NotificationLevel> levels, JsonNode config) {

    public static final SharedKind<ChannelAttributes> KIND =
            SharedKind.of("M13.NOTIFICATION_CHANNEL", ChannelAttributes.class).notDeletable();
}
