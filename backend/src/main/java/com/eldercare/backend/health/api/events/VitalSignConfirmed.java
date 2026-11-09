package com.eldercare.backend.health.api.events;

import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.time.Instant;
import java.util.Set;

/**
 * Chỉ số vừa xác nhận; giữ phiên bản ngưỡng đã áp tại lúc đo (FR-013, FR-014). {@code recordedById} là người ghi bản đo,
 * dùng làm "nhân viên thực hiện công việc nguồn" của thông báo cảnh báo Trung bình (007 FR-035); có thể rỗng.
 */
public record VitalSignConfirmed(
        long recordId,
        long residentId,
        long indicatorId,
        VitalSignLevel level,
        Long thresholdVersionId,
        Long taskId,
        Instant measuredAt,
        Set<VitalSignLabel> labels,
        String eventKey,
        Instant occurredAt,
        Long recordedById) {
    public VitalSignConfirmed {
        labels = Set.copyOf(labels);
    }

    public VitalSignConfirmed(
            long recordId,
            long residentId,
            long indicatorId,
            VitalSignLevel level,
            Long thresholdVersionId,
            Long taskId,
            Instant measuredAt,
            Set<VitalSignLabel> labels,
            String eventKey,
            Instant occurredAt) {
        this(
                recordId,
                residentId,
                indicatorId,
                level,
                thresholdVersionId,
                taskId,
                measuredAt,
                labels,
                eventKey,
                occurredAt,
                null);
    }
}
