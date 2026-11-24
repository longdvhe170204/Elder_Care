package com.eldercare.backend.staffing.internal.handover.domain;

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
        Instant at,
        Boolean nextStepMissing) {
    public HandoverSource(
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
        this(
                group,
                sourceType,
                sourceId,
                residentId,
                roomId,
                description,
                currentStatus,
                critical,
                hasItem,
                dataUnavailable,
                level,
                at,
                null);
    }

    /** Sự cố đang mở chưa ghi việc tiếp theo (007 FR-045b). */
    public HandoverSource withNextStepMissing(Boolean missing) {
        return new HandoverSource(
                group,
                sourceType,
                sourceId,
                residentId,
                roomId,
                description,
                currentStatus,
                critical,
                hasItem,
                dataUnavailable,
                level,
                at,
                missing);
    }

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
                at,
                nextStepMissing);
    }
}
