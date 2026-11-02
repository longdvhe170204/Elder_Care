package com.eldercare.backend.roombedallocation.api.dto;

import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Metadata thông báo do lệnh nguồn xác định: mốc sự kiện, tầng và nhóm người nhận theo G-12. */
public record BedNotificationContext(Instant occurredAt, Long floorAreaId, List<RecipientGroup> recipients) {
    public BedNotificationContext {
        Objects.requireNonNull(occurredAt);
        recipients = List.copyOf(recipients);
        if (recipients.isEmpty()) throw new IllegalArgumentException("Thông báo cần nhóm người nhận theo G-12");
    }
}
