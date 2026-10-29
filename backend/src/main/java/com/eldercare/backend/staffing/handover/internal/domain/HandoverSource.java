package com.eldercare.backend.staffing.handover.internal.domain;

import java.time.Instant;

/** Dữ liệu tham khảo, không phải mục bàn giao; level/at chỉ dùng nội bộ. */
public record HandoverSource(
        String group,
        String sourceType,
        String sourceId,
        Long residentId,
        Long roomId,
        String description,
        String currentStatus,
        boolean critical,
        boolean hasItem,
        boolean dataUnavailable,
        String level,
        Instant at) {
    public HandoverSource marked(boolean included) {
        return new HandoverSource(
                group,
                sourceType,
                sourceId,
                residentId,
                roomId,
                description,
                currentStatus,
                critical,
                included,
                dataUnavailable,
                level,
                at);
    }
}
