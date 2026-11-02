package com.eldercare.backend.health.internal.measurement.domain;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** Bản đọc bất biến; dòng chờ đo lại được khóa đúng một lần khi xác nhận (R-05). */
@DataGroup(DataGroup.Group.MULTIPLE)
public record VitalSignRecord(
        long id,
        long residentId,
        long indicatorId,
        Instant measuredAt,
        long measuredBy,
        Long taskId,
        Instant deviceMeasuredAt,
        boolean offline,
        Long thresholdVersionId,
        VitalSignLevel level,
        VitalSignConfirmationStatus confirmationStatus,
        Instant remeasurementDeadline,
        Long remeasurementFor,
        Map<String, VitalSignComponent> components,
        Set<VitalSignLabel> labels,
        String note,
        Instant confirmedAt,
        Instant lockedAt,
        Instant createdAt,
        long createdBy) {
    public VitalSignRecord {
        components = components != null ? Map.copyOf(components) : Map.of();
        labels = labels != null ? Set.copyOf(labels) : Set.of();
    }

    public VitalSignRecord(
            long id,
            long residentId,
            long indicatorId,
            Instant measuredAt,
            VitalSignLevel level,
            VitalSignConfirmationStatus confirmationStatus,
            Map<String, VitalSignComponent> components,
            Set<VitalSignLabel> labels,
            Instant lockedAt) {
        this(
                id,
                residentId,
                indicatorId,
                measuredAt,
                0L,
                null,
                null,
                false,
                null,
                level,
                confirmationStatus,
                null,
                null,
                components,
                labels,
                null,
                lockedAt,
                lockedAt,
                Instant.EPOCH,
                0L);
    }
}
